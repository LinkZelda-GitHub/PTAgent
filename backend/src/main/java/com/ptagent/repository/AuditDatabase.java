package com.ptagent.repository;

import com.ptagent.common.Json;
import com.ptagent.domain.AuditLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

final class AuditDatabase {
    static final String FILE_NAME = "ptagent-audit.jsonl";

    private final Path file;

    AuditDatabase(Path dataDirectory) {
        this.file = dataDirectory.toAbsolutePath().normalize().resolve(FILE_NAME);
    }

    synchronized List<AuditLog> read() {
        if (!Files.exists(file)) {
            return List.of();
        }
        List<AuditLog> logs = new ArrayList<>();
        String expectedPreviousHash = "";
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                AuditLog log = fromMap(Json.parseObject(line));
                if (!expectedPreviousHash.equals(log.previousHash) || !hash(log).equals(log.hash)) {
                    throw new IllegalStateException("审计日志哈希链校验失败：" + file);
                }
                logs.add(log);
                expectedPreviousHash = log.hash;
            }
            return logs;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("无法读取审计日志：" + file, exception);
        }
    }

    synchronized void append(AuditLog log, String previousHash) {
        log.previousHash = previousHash == null ? "" : previousHash;
        log.hash = hash(log);
        byte[] bytes = (Json.stringify(log.toMap()) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        try {
            Files.createDirectories(file.getParent());
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法写入审计日志：" + file, exception);
        }
    }

    String location() {
        return file.toString();
    }

    boolean healthy() {
        Path directory = file.getParent();
        return (Files.notExists(file) || Files.isReadable(file))
                && (Files.notExists(directory) || Files.isWritable(directory));
    }

    private AuditLog fromMap(Map<String, Object> item) {
        AuditLog log = new AuditLog();
        log.id = Json.longValue(item, "id", 0);
        log.actorId = Json.longValue(item, "actorId", 0);
        log.actorRole = Json.str(item, "actorRole");
        log.action = Json.str(item, "action");
        log.targetType = Json.str(item, "targetType");
        log.targetId = Json.longValue(item, "targetId", 0);
        log.detail = Json.str(item, "detail");
        log.requestId = Json.str(item, "requestId");
        log.clientIp = Json.str(item, "clientIp");
        log.userAgent = Json.str(item, "userAgent");
        log.result = Json.str(item, "result");
        log.previousHash = Json.str(item, "previousHash");
        log.hash = Json.str(item, "hash");
        log.createTime = LocalDateTime.parse(Json.str(item, "createTime"));
        if (log.id <= 0 || log.action.isBlank() || log.result.isBlank() || log.hash.isBlank()) {
            throw new IllegalStateException("审计日志字段不完整：" + file);
        }
        return log;
    }

    private String hash(AuditLog log) {
        String canonical = log.previousHash + "\n" + Json.stringify(log.contentMap());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK不支持SHA-256", exception);
        }
    }
}
