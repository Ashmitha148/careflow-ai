package com.careflow.ai.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.FileAttachment;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.FileAttachmentRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileAttachmentService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public FileAttachmentService(
            FileAttachmentRepository fileAttachmentRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            Cloudinary cloudinary,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.cloudinary = cloudinary;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FileAttachment uploadFile(
            UUID patientId,
            UUID uploadedByUserId,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "File size cannot exceed 10 MB");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Patient not found: " + patientId));

        User uploadedBy = userRepository.findById(uploadedByUserId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + uploadedByUserId));

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", "careflow/patients/" + patientId
                    )
            );

            String cloudinaryUrl =
                    String.valueOf(result.get("secure_url"));

            FileAttachment attachment = new FileAttachment();
            attachment.setPatient(patient);
            attachment.setUploadedBy(uploadedBy);
            attachment.setCloudinaryUrl(cloudinaryUrl);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setMimeType(
                    file.getContentType() != null
                            ? file.getContentType()
                            : "application/octet-stream");
            attachment.setSize(file.getSize());

            FileAttachment saved =
                    fileAttachmentRepository.save(attachment);

            String description =
                    "File uploaded: " + file.getOriginalFilename();

            timelineService.appendEvent(
                    patientId,
                    EventType.FILE_UPLOAD,
                    description,
                    uploadedByUserId
            );

            auditLogService.log(
                    uploadedByUserId,
                    "UPLOAD_FILE",
                    "FileAttachment",
                    saved.getId(),
                    description
            );

            return saved;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload file to Cloudinary", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FileAttachment> getPatientFiles(UUID patientId) {
        return fileAttachmentRepository
                .findByPatient_IdOrderByUploadedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public FileAttachment getFile(UUID fileId) {
        return fileAttachmentRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "File attachment not found: " + fileId));
    }

    @Transactional
    public void deleteFile(UUID fileId, UUID deletedByUserId) {

        FileAttachment attachment = fileAttachmentRepository
                .findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "File attachment not found: " + fileId));

        try {
            String url = attachment.getCloudinaryUrl();

            String publicId = extractPublicId(url);

            if (publicId != null && !publicId.isBlank()) {
                cloudinary.uploader().destroy(
                        publicId,
                        ObjectUtils.asMap("resource_type", "auto")
                );
            }

            fileAttachmentRepository.delete(attachment);

            String description =
                    "File deleted: " + attachment.getFileName();

            timelineService.appendEvent(
                    attachment.getPatient().getId(),
                    EventType.FILE_UPLOAD,
                    description,
                    deletedByUserId
            );

            auditLogService.log(
                    deletedByUserId,
                    "DELETE_FILE",
                    "FileAttachment",
                    fileId,
                    description
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to delete file from Cloudinary", e);
        }
    }

    private String extractPublicId(String url) {

        if (url == null || url.isBlank()) {
            return null;
        }

        int uploadIndex = url.indexOf("/upload/");

        if (uploadIndex < 0) {
            return null;
        }

        String path = url.substring(uploadIndex + 8);

        if (path.startsWith("v")) {
            int slashIndex = path.indexOf('/');

            if (slashIndex > 0) {
                path = path.substring(slashIndex + 1);
            }
        }

        int extensionIndex = path.lastIndexOf('.');

        if (extensionIndex > 0) {
            path = path.substring(0, extensionIndex);
        }

        return path;
    }
}
