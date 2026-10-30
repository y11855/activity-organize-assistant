package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.entity.ActivityFile;
import com.aiapp.activityassistant.service.FileDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件下载实现。
 *
 * <p>断点续传依赖标准 HTTP Range：客户端带 {@code Range: bytes=start-} 时返回 206 与 Content-Range，
 * 暂停后再次请求即可从断点继续；同时返回 ETag（文件 SHA-256）与 Accept-Ranges，浏览器可做缓存与多段并发下载。</p>
 */
@Slf4j
@Service
public class FileDownloadServiceImpl implements FileDownloadService {

    private static final int BUFFER_SIZE = 8192;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void writeFile(ActivityFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Path path = resolve(file.getStoragePath());
        if (!Files.exists(path)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件已丢失");
        }
        long length = Files.size(path);
        String etag = file.getSha256() == null ? null : "\"" + file.getSha256() + "\"";
        if (etag != null && etag.equals(request.getHeader("If-None-Match"))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setHeader("Accept-Ranges", "bytes");
        if (etag != null) {
            response.setHeader("ETag", etag);
        }
        response.setContentType(contentType(path));
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encode(file.getName()));

        long start = 0;
        long end = length - 1;
        String range = request.getHeader("Range");
        if (range != null && range.startsWith("bytes=")) {
            // 单段 Range 即可满足断点续传；多段并发下载由客户端拆成多个单段请求
            String[] parts = range.substring("bytes=".length()).split("-", -1);
            try {
                if (parts[0].isBlank()) {
                    long suffix = Long.parseLong(parts[1].trim());
                    start = Math.max(0, length - suffix);
                } else {
                    start = Long.parseLong(parts[0].trim());
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        end = Math.min(Long.parseLong(parts[1].trim()), length - 1);
                    }
                }
            } catch (NumberFormatException e) {
                start = 0;
                end = length - 1;
            }
            if (start >= length) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + length);
                return;
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
        }

        long contentLength = end - start + 1;
        response.setContentLengthLong(contentLength);
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(start);
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = contentLength;
            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
            out.flush();
        }
    }

    @Override
    public void writeZip(String zipName, List<ActivityFile> files, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encode(zipName));

        // 边读边压缩写回响应，不落临时文件：内存占用小，多人同时打包也不会堆积磁盘
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            Set<String> usedNames = new HashSet<>();
            for (ActivityFile file : files) {
                Path path = resolve(file.getStoragePath());
                if (!Files.exists(path)) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(uniqueName(file.getName(), usedNames)));
                Files.copy(path, zos);
                zos.closeEntry();
            }
        }
    }

    private String uniqueName(String name, Set<String> usedNames) {
        if (usedNames.add(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        String ext = dot < 0 ? "" : name.substring(dot);
        for (int i = 2; ; i++) {
            String candidate = base + "(" + i + ")" + ext;
            if (usedNames.add(candidate)) {
                return candidate;
            }
        }
    }

    private Path resolve(String storagePath) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(storagePath).normalize();
        // 防止 storagePath 被篡改导致路径穿越
        if (!path.startsWith(root)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非法的文件路径");
        }
        return path;
    }

    private String contentType(Path path) {
        try {
            String type = Files.probeContentType(path);
            return type == null ? "application/octet-stream" : type;
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String encode(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
