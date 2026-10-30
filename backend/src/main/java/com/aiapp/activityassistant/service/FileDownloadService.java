package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.entity.ActivityFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 文件下载：HTTP Range 断点续传 + 多文件流式打包
 */
public interface FileDownloadService {

    /** 支持 Range 的文件下载，返回 200 / 206 / 304 / 416 */
    void writeFile(ActivityFile file, HttpServletRequest request, HttpServletResponse response) throws IOException;

    /** 边压缩边输出，不产生临时文件，适合多人同时打包下载 */
    void writeZip(String zipName, List<ActivityFile> files, HttpServletResponse response) throws IOException;
}
