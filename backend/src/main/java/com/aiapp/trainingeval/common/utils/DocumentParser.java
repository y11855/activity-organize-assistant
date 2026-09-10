package com.aiapp.trainingeval.common.utils;

import com.aiapp.trainingeval.common.exception.BusinessException;
import com.aiapp.trainingeval.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档解析工具：支持 docx / pdf / txt
 */
@Slf4j
@Component
public class DocumentParser {

    /**
     * 解析文件为纯文本
     */
    public String parse(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在: " + filePath);
            }
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".docx")) {
                return parseDocx(filePath);
            } else if (name.endsWith(".pdf")) {
                return parsePdf(filePath);
            } else if (name.endsWith(".txt")) {
                return new String(FileCopyUtils.copyToByteArray(new FileInputStream(filePath)));
            } else if (name.matches(".*\\.(png|jpg|jpeg)$")) {
                // 图片走 OCR，此处占位
                return "[图片内容需 OCR 解析]";
            }
            throw new BusinessException(ResultCode.FILE_PARSE_ERROR, "不支持的文件类型: " + name);
        } catch (Exception e) {
            log.error("文件解析失败: {}", filePath, e);
            throw new BusinessException(ResultCode.FILE_PARSE_ERROR, e.getMessage());
        }
    }

    private String parseDocx(String filePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    private String parsePdf(String filePath) throws Exception {
        try (PDDocument doc = Loader.loadPDF(new java.io.File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}
