package com.nym.shortlink.core.toolkit;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 封装 EasyExcel 操作 Web 工具方法
 * 该类提供了将数据导出为 Excel 文件并直接响应给浏览器的工具方法
 */
public class EasyExcelWebUtil {

    /**
     * 向浏览器写入 Excel 响应，直接返回用户下载数据
     * 该方法设置了响应头，使浏览器能够以下载方式接收 Excel 文件

     *
     * @param response 响应对象，用于向客户端发送响应数据
     * @param fileName 文件名，将作为下载时的默认文件名
     * @param clazz    指定写入类，Excel 表格将按照该类的结构进行映射
     * @param data     写入数据，需要导出到 Excel 的数据列表
     * @throws IOException 当发生 IO 异常时抛出
     */
    @SneakyThrows
    public static void write(HttpServletResponse response, String fileName, Class<?> clazz, List<?> data) {
        // 设置响应内容类型为 Excel 文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 设置响应字符编码为 UTF-8
        response.setCharacterEncoding("utf-8");
        // 对文件名进行 URL 编码，确保中文文件名能够正确显示
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        // 设置响应头，指定文件为附件形式下载，并设置文件名
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        // 使用 EasyExcel 写入数据到输出流，指定工作表名为 "Sheet"
        EasyExcel.write(response.getOutputStream(), clazz).sheet("Sheet").doWrite(data);
    }
}
