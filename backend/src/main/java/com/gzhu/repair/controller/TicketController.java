package com.gzhu.repair.controller;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.PageResult;
import com.gzhu.repair.common.Result;
import com.gzhu.repair.dto.EvaluationRequest;
import com.gzhu.repair.dto.TicketActionRequest;
import com.gzhu.repair.dto.TicketCreateRequest;
import com.gzhu.repair.entity.Attachment;
import com.gzhu.repair.entity.Evaluation;
import com.gzhu.repair.entity.Ticket;
import com.gzhu.repair.repository.AttachmentRepository;
import com.gzhu.repair.service.FileStorageService;
import com.gzhu.repair.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    public TicketController(TicketService ticketService,
                            AttachmentRepository attachmentRepository,
                            FileStorageService fileStorageService) {
        this.ticketService = ticketService;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/tickets")
    public Result<PageResult<Ticket>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(ticketService.list(status, category, buildingId, keyword,
                parseDate(start), parseDate(end), page, size));
    }

    @PostMapping("/tickets")
    public Result<Ticket> create(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success(ticketService.create(request));
    }

    @GetMapping("/tickets/{id}")
    public Result<Ticket> detail(@PathVariable Long id) {
        return Result.success(ticketService.detail(id));
    }

    @PostMapping("/tickets/{id}/actions")
    public Result<Ticket> action(@PathVariable Long id, @Valid @RequestBody TicketActionRequest request) {
        return Result.success(ticketService.action(id, request));
    }

    @PostMapping("/tickets/{id}/evaluation")
    public Result<Evaluation> evaluate(@PathVariable Long id, @Valid @RequestBody EvaluationRequest request) {
        return Result.success(ticketService.evaluate(id, request));
    }

    @PostMapping("/tickets/{id}/attachments")
    public Result<Attachment> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(ticketService.addAttachment(id, file));
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("附件不存在"));
        ticketService.requireVisible(attachment.getTicketId());
        Path path = fileStorageService.resolve(attachment.getFilePath());
        Resource resource = new FileSystemResource(path);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getMimeType() != null) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getMimeType());
            } catch (Exception ignored) {
                // 使用默认类型
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/admin/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        byte[] data = ticketService.exportCsv(status, category, buildingId, keyword,
                parseDate(start), parseDate(end));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest("日期格式应为 yyyy-MM-dd");
        }
    }
}
