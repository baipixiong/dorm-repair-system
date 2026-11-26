package com.gzhu.repair.service;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Constants;
import com.gzhu.repair.common.PageResult;
import com.gzhu.repair.dto.EvaluationRequest;
import com.gzhu.repair.dto.TicketActionRequest;
import com.gzhu.repair.dto.TicketCreateRequest;
import com.gzhu.repair.entity.Attachment;
import com.gzhu.repair.entity.Building;
import com.gzhu.repair.entity.Evaluation;
import com.gzhu.repair.entity.Ticket;
import com.gzhu.repair.entity.TicketEvent;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.AttachmentRepository;
import com.gzhu.repair.repository.BuildingRepository;
import com.gzhu.repair.repository.EvaluationRepository;
import com.gzhu.repair.repository.TicketEventRepository;
import com.gzhu.repair.repository.TicketRepository;
import com.gzhu.repair.repository.UserRepository;
import com.gzhu.repair.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketRepository ticketRepository;
    private final TicketEventRepository eventRepository;
    private final AttachmentRepository attachmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public TicketService(TicketRepository ticketRepository,
                         TicketEventRepository eventRepository,
                         AttachmentRepository attachmentRepository,
                         EvaluationRepository evaluationRepository,
                         UserRepository userRepository,
                         BuildingRepository buildingRepository,
                         FileStorageService fileStorageService,
                         AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.attachmentRepository = attachmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    @Transactional
    public Ticket create(TicketCreateRequest request) {
        User user = UserContext.require();
        if (!Constants.ROLE_STUDENT.equals(user.getRole())) {
            throw BusinessException.forbidden("只有学生可以提交报修");
        }
        validateCategory(request.getCategory());
        String priority = request.getPriority() == null || request.getPriority().isBlank()
                ? Constants.PRIORITY_NORMAL : request.getPriority();
        validatePriority(priority);
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> BusinessException.badRequest("报修楼栋不存在"));
        String roomNo = request.getRoomNo() == null ? "" : request.getRoomNo().trim();
        if (roomNo.isEmpty()) {
            throw BusinessException.badRequest("请输入房间号");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setStudentId(user.getId());
        ticket.setBuildingId(building.getId());
        ticket.setRoomNo(roomNo);
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(priority);
        ticket.setStatus(Constants.STATUS_PENDING);
        String phone = request.getContactPhone();
        ticket.setContactPhone(phone == null || phone.isBlank() ? user.getPhone() : phone.trim());
        ticketRepository.saveAndFlush(ticket);

        addEvent(ticket, user, "create", "", Constants.STATUS_PENDING, "学生提交报修");
        auditService.log(user, "提交报修", "创建工单 " + ticket.getTicketNo());
        enrichDetail(ticket);
        return ticket;
    }

    public PageResult<Ticket> list(String status,
                                   String category,
                                   Long buildingId,
                                   String keyword,
                                   LocalDate start,
                                   LocalDate end,
                                   int page,
                                   int size) {
        User user = UserContext.require();
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.atTime(23, 59, 59);
        Specification<Ticket> spec = TicketSpecs.scoped(user)
                .and(TicketSpecs.filter(status, category, buildingId, keyword, startTime, endTime));
        Page<Ticket> result = ticketRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        enrichList(result.getContent());
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, size);
    }

    public Ticket detail(Long id) {
        User user = UserContext.require();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("工单不存在"));
        checkView(ticket, user);
        enrichDetail(ticket);
        return ticket;
    }

    public Ticket requireVisible(Long id) {
        return detail(id);
    }

    @Transactional
    public Ticket action(Long id, TicketActionRequest request) {
        User user = UserContext.require();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("工单不存在"));
        checkView(ticket, user);
        if (request.getVersion() == null || !request.getVersion().equals(ticket.getVersion())) {
            throw BusinessException.conflict("工单已被其他人更新，请刷新后重试");
        }

        String action = request.getAction();
        String from = ticket.getStatus();
        String to = from;
        String note = request.getNote() == null ? null : request.getNote().trim();

        switch (action) {
            case "cancel" -> {
                requireStudentOwner(ticket, user);
                if (!Constants.STATUS_PENDING.equals(from)) {
                    throw BusinessException.conflict("只有待派单的工单可以取消");
                }
                to = Constants.STATUS_CANCELLED;
                if (note == null || note.isBlank()) {
                    note = "学生取消报修";
                }
            }
            case "assign" -> {
                requireAdmin(user);
                if (!(Constants.STATUS_PENDING.equals(from)
                        || Constants.STATUS_ASSIGNED.equals(from)
                        || Constants.STATUS_WORKING.equals(from))) {
                    throw BusinessException.conflict("当前状态的工单不能派单");
                }
                if (request.getAssigneeId() == null) {
                    throw BusinessException.badRequest("请选择维修人员");
                }
                User technician = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> BusinessException.badRequest("维修人员不存在"));
                if (!Constants.ROLE_TECHNICIAN.equals(technician.getRole())
                        || technician.getStatus() == null || technician.getStatus() != 1) {
                    throw BusinessException.badRequest("该账号不是可用的维修人员");
                }
                ticket.setAssigneeId(technician.getId());
                if (request.getScheduledAt() != null) {
                    ticket.setScheduledAt(request.getScheduledAt());
                }
                ticket.setFinishedAt(null);
                ticket.setCompletedAt(null);
                to = Constants.STATUS_ASSIGNED;
                if (note == null || note.isBlank()) {
                    note = "已派单给 " + technician.getRealName();
                }
            }
            case "start" -> {
                requireAssigneeTechnician(ticket, user);
                if (!Constants.STATUS_ASSIGNED.equals(from)) {
                    throw BusinessException.conflict("只有已派单的工单可以开始维修");
                }
                to = Constants.STATUS_WORKING;
                if (note == null || note.isBlank()) {
                    note = "维修人员开始处理工单";
                }
            }
            case "finish" -> {
                requireAssigneeTechnician(ticket, user);
                if (!Constants.STATUS_WORKING.equals(from)) {
                    throw BusinessException.conflict("只有维修中的工单可以提交完工");
                }
                ticket.setFinishedAt(LocalDateTime.now());
                to = Constants.STATUS_REVIEW;
                if (note == null || note.isBlank()) {
                    note = "维修完成，请学生验收";
                }
            }
            case "accept" -> {
                requireStudentOwner(ticket, user);
                if (!Constants.STATUS_REVIEW.equals(from)) {
                    throw BusinessException.conflict("只有待验收的工单可以验收");
                }
                ticket.setCompletedAt(LocalDateTime.now());
                to = Constants.STATUS_COMPLETED;
                if (note == null || note.isBlank()) {
                    note = "学生验收通过";
                }
            }
            case "reject" -> {
                requireStudentOwner(ticket, user);
                if (!Constants.STATUS_REVIEW.equals(from)) {
                    throw BusinessException.conflict("只有待验收的工单可以申请返修");
                }
                if (note == null || note.isBlank()) {
                    throw BusinessException.badRequest("请填写返修原因");
                }
                ticket.setFinishedAt(null);
                to = Constants.STATUS_WORKING;
            }
            case "comment" -> {
                if (note == null || note.isBlank()) {
                    throw BusinessException.badRequest("请填写备注内容");
                }
                to = from;
            }
            case "classify" -> {
                requireAdmin(user);
                if (!Constants.isActiveStatus(from)) {
                    throw BusinessException.conflict("已结束的工单不能修改分类");
                }
                validateCategory(request.getCategory());
                ticket.setCategory(request.getCategory());
                to = from;
                if (note == null || note.isBlank()) {
                    note = "分类调整为 " + Constants.CATEGORY_LABELS.get(request.getCategory());
                }
            }
            default -> throw BusinessException.badRequest("不支持的操作类型");
        }

        ticket.setStatus(to);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.saveAndFlush(ticket);
        addEvent(ticket, user, action, from, to, note);
        auditService.log(user, "工单操作", "工单 " + ticket.getTicketNo() + " 执行 " + action);
        enrichDetail(ticket);
        return ticket;
    }

    @Transactional
    public Evaluation evaluate(Long id, EvaluationRequest request) {
        User user = UserContext.require();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("工单不存在"));
        requireStudentOwner(ticket, user);
        if (!Constants.STATUS_COMPLETED.equals(ticket.getStatus())) {
            throw BusinessException.conflict("只有已完成的工单可以评价");
        }
        if (evaluationRepository.existsByTicketId(id)) {
            throw BusinessException.conflict("该工单已经评价过了");
        }
        Evaluation evaluation = new Evaluation();
        evaluation.setTicketId(id);
        evaluation.setStudentId(user.getId());
        evaluation.setScore(request.getScore());
        evaluation.setComment(request.getComment());
        evaluationRepository.save(evaluation);
        addEvent(ticket, user, "evaluate", ticket.getStatus(), ticket.getStatus(),
                "学生评价 " + request.getScore() + " 星" +
                        (request.getComment() == null || request.getComment().isBlank()
                                ? "" : "：" + request.getComment()));
        auditService.log(user, "工单评价", "工单 " + ticket.getTicketNo() + " 评分 " + request.getScore());
        evaluation.setStudentName(user.getRealName());
        return evaluation;
    }

    @Transactional
    public Attachment addAttachment(Long id, MultipartFile file) {
        User user = UserContext.require();
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("工单不存在"));
        checkView(ticket, user);
        if (Constants.STATUS_COMPLETED.equals(ticket.getStatus())
                || Constants.STATUS_CANCELLED.equals(ticket.getStatus())) {
            throw BusinessException.conflict("已结束的工单不能上传图片");
        }
        if (attachmentRepository.countByTicketId(id) >= 5) {
            throw BusinessException.conflict("每张工单最多上传 5 张图片");
        }
        Attachment attachment = fileStorageService.store(id, user.getId(), file);
        attachment.setUploaderName(user.getRealName());
        auditService.log(user, "上传附件", "工单 " + ticket.getTicketNo() + " 上传图片 " + attachment.getFileName());
        return attachment;
    }

    public byte[] exportCsv(String status,
                            String category,
                            Long buildingId,
                            String keyword,
                            LocalDate start,
                            LocalDate end) {
        User user = UserContext.require();
        requireAdmin(user);
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.atTime(23, 59, 59);
        Specification<Ticket> spec = TicketSpecs.scoped(user)
                .and(TicketSpecs.filter(status, category, buildingId, keyword, startTime, endTime));
        List<Ticket> tickets = ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        enrichList(tickets);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("工单编号,标题,类别,状态,优先级,楼栋,房间,报修人,联系电话,维修人员,创建时间,验收时间\n");
        for (Ticket ticket : tickets) {
            sb.append(csv(ticket.getTicketNo())).append(',')
                    .append(csv(ticket.getTitle())).append(',')
                    .append(csv(ticket.getCategoryLabel())).append(',')
                    .append(csv(ticket.getStatusLabel())).append(',')
                    .append(csv(ticket.getPriorityLabel())).append(',')
                    .append(csv(ticket.getBuildingName())).append(',')
                    .append(csv(ticket.getRoomNo())).append(',')
                    .append(csv(ticket.getStudentName())).append(',')
                    .append(csv(ticket.getContactPhone())).append(',')
                    .append(csv(ticket.getAssigneeName())).append(',')
                    .append(csv(ticket.getCreatedAt() == null ? "" : ticket.getCreatedAt().toString())).append(',')
                    .append(csv(ticket.getCompletedAt() == null ? "" : ticket.getCompletedAt().toString()))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public void checkView(Ticket ticket, User user) {
        if (Constants.ROLE_ADMIN.equals(user.getRole())) {
            return;
        }
        if (Constants.ROLE_STUDENT.equals(user.getRole())
                && ticket.getStudentId().equals(user.getId())) {
            return;
        }
        if (Constants.ROLE_TECHNICIAN.equals(user.getRole())
                && user.getId().equals(ticket.getAssigneeId())) {
            return;
        }
        throw BusinessException.notFound("工单不存在或无权查看");
    }

    public void enrichDetail(Ticket ticket) {
        enrichList(Collections.singletonList(ticket));

        List<TicketEvent> events = eventRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId());
        if (!events.isEmpty()) {
            Set<Long> actorIds = events.stream().map(TicketEvent::getActorId).collect(Collectors.toSet());
            Map<Long, User> actors = userRepository.findAllById(actorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            for (TicketEvent event : events) {
                User actor = actors.get(event.getActorId());
                if (actor != null) {
                    event.setActorName(actor.getRealName());
                    event.setActorRole(Constants.ROLE_LABELS.get(actor.getRole()));
                }
            }
        }
        ticket.setEvents(events);

        List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        if (!attachments.isEmpty()) {
            Set<Long> uploaderIds = attachments.stream().map(Attachment::getUploaderId).collect(Collectors.toSet());
            Map<Long, User> uploaders = userRepository.findAllById(uploaderIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            for (Attachment attachment : attachments) {
                User uploader = uploaders.get(attachment.getUploaderId());
                if (uploader != null) {
                    attachment.setUploaderName(uploader.getRealName());
                }
                attachment.setUrl("/api/attachments/" + attachment.getId());
            }
        }
        ticket.setAttachments(attachments);

        evaluationRepository.findByTicketId(ticket.getId()).ifPresent(evaluation -> {
            userRepository.findById(evaluation.getStudentId())
                    .ifPresent(student -> evaluation.setStudentName(student.getRealName()));
            ticket.setEvaluation(evaluation);
        });
    }

    private void enrichList(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> buildingIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            if (ticket.getStudentId() != null) {
                userIds.add(ticket.getStudentId());
            }
            if (ticket.getAssigneeId() != null) {
                userIds.add(ticket.getAssigneeId());
            }
            if (ticket.getBuildingId() != null) {
                buildingIds.add(ticket.getBuildingId());
            }
        }
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Building> buildings = buildingRepository.findAllById(buildingIds).stream()
                .collect(Collectors.toMap(Building::getId, b -> b));

        for (Ticket ticket : tickets) {
            User student = users.get(ticket.getStudentId());
            if (student != null) {
                ticket.setStudentName(student.getRealName());
                ticket.setStudentNo(student.getStudentNo());
                ticket.setStudentPhone(student.getPhone());
            }
            User assignee = ticket.getAssigneeId() == null ? null : users.get(ticket.getAssigneeId());
            if (assignee != null) {
                ticket.setAssigneeName(assignee.getRealName());
                ticket.setAssigneePhone(assignee.getPhone());
            }
            Building building = buildings.get(ticket.getBuildingId());
            if (building != null) {
                ticket.setBuildingName(building.getName());
            }
            ticket.setCategoryLabel(Constants.CATEGORY_LABELS.getOrDefault(ticket.getCategory(), ticket.getCategory()));
            ticket.setStatusLabel(Constants.STATUS_LABELS.getOrDefault(ticket.getStatus(), ticket.getStatus()));
            ticket.setPriorityLabel(Constants.PRIORITY_LABELS.getOrDefault(ticket.getPriority(), ticket.getPriority()));
        }
    }

    private void addEvent(Ticket ticket, User actor, String action, String from, String to, String note) {
        TicketEvent event = new TicketEvent();
        event.setTicketId(ticket.getId());
        event.setActorId(actor.getId());
        event.setAction(action);
        event.setFromStatus(from == null ? "" : from);
        event.setToStatus(to == null ? "" : to);
        event.setNote(note);
        eventRepository.save(event);
    }

    private String nextTicketNo() {
        String date = LocalDate.now().format(DAY_FORMAT);
        String no;
        do {
            no = "BX" + date + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        } while (ticketRepository.existsByTicketNo(no));
        return no;
    }

    private void validateCategory(String category) {
        if (category == null || !Constants.CATEGORY_LABELS.containsKey(category)) {
            throw BusinessException.badRequest("报修类别不合法");
        }
    }

    private void validatePriority(String priority) {
        if (!Constants.PRIORITY_LABELS.containsKey(priority)) {
            throw BusinessException.badRequest("优先级不合法");
        }
    }

    private void requireAdmin(User user) {
        if (!Constants.ROLE_ADMIN.equals(user.getRole())) {
            throw BusinessException.forbidden("仅管理员可以执行该操作");
        }
    }

    private void requireStudentOwner(Ticket ticket, User user) {
        if (!Constants.ROLE_STUDENT.equals(user.getRole()) || !ticket.getStudentId().equals(user.getId())) {
            throw BusinessException.forbidden("只有报修学生本人可以执行该操作");
        }
    }

    private void requireAssigneeTechnician(Ticket ticket, User user) {
        if (!Constants.ROLE_TECHNICIAN.equals(user.getRole())
                || !user.getId().equals(ticket.getAssigneeId())) {
            throw BusinessException.forbidden("只有该工单的维修人员可以执行此操作");
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text + "\"";
        }
        return text;
    }
}
