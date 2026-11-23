package com.gzhu.repair.service;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.entity.Attachment;
import com.gzhu.repair.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final AttachmentRepository attachmentRepository;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public FileStorageService(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public Attachment store(Long ticketId, Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.badRequest("只能上传图片文件");
        }
        String original = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw BusinessException.badRequest("仅支持 jpg、png、gif、webp、bmp 格式图片");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = "tickets/" + ticketId + "/" + storedName;
        Path target = baseDir().resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败，请重试");
        }

        Attachment attachment = new Attachment();
        attachment.setTicketId(ticketId);
        attachment.setUploaderId(uploaderId);
        attachment.setFileName(original);
        attachment.setFilePath(relativePath);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(contentType);
        attachmentRepository.save(attachment);
        attachment.setUrl("/api/attachments/" + attachment.getId());
        return attachment;
    }

    public Path resolve(String relativePath) {
        Path base = baseDir();
        Path path = base.resolve(relativePath).normalize();
        if (!path.startsWith(base)) {
            throw BusinessException.forbidden("非法的文件路径");
        }
        if (!Files.exists(path)) {
            throw BusinessException.notFound("文件不存在");
        }
        return path;
    }

    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(baseDir().resolve(relativePath).normalize());
        } catch (IOException ignored) {
            // 文件删除失败不影响主流程
        }
    }

    private Path baseDir() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
