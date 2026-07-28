package com.md.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.MinioClient;
import io.minio.messages.Bucket;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * @Author: CM
 * @Description TODO
 */

public class MinioUtil {

    private static MinioClient minioClient;
    private final static String END_POINT="http://192.168.48.130:9001";
    private final static String ACCESS_KEY="pXscBebw12ZnKazCqhIb";
    private final static String SECRET_KEY="Zf99dQIJQguzSGEj3SLYwe5KlNKOw2VRttFcCbrJ";


    static {
        try {
            minioClient = new MinioClient(END_POINT, ACCESS_KEY, SECRET_KEY);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 将文件上传到MinIO对象存储服务中
     *
     * @param file       文件对象
     * @param fileName   文件名称
     * @param dir        文件存储目录
     * @param bucketName MinIO桶名
     */
    @SneakyThrows
    public static void upload(MultipartFile file,
                              String fileName,
                              String dir,
                              String bucketName) {
        checkMultipartFile(file);
        checkFileName(fileName);
        checkDir(dir);
        checkBucket(bucketName);
        // 使用putObject上传文件到指定Bucket
        minioClient.putObject(bucketName, dir + "/" + fileName,
                file.getInputStream(),
                file.getContentType());
    }

    /**
     * 根据文件对象获取随机文件名称
     *
     * @param file 文件对象
     * @return 随机文件名称，格式为 "UUID后10位 + 当前时间戳 + 原始文件后缀"
     */
    public static String randomFilename(MultipartFile file) {
        checkMultipartFile(file);
        // 获取原始文件名
        String fileName = file.getOriginalFilename();
        checkFileName(fileName);
        // 生成随机文件名并返回
        return UUID.randomUUID().toString().substring(26) + "-"
                + System.currentTimeMillis()
                + fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 从MinIO中删除指定文件
     *
     * @param fileName   文件名
     * @param dir        文件存储目录
     * @param bucketName MinIO桶名
     */
    @SneakyThrows
    public static void delete(String fileName,
                              String dir,
                              String bucketName) {
        checkFileName(fileName);
        checkDir(dir);
        checkBucket(bucketName);
        minioClient.removeObject(bucketName, dir + "/" + fileName);
    }

    /**
     * 检查 MultipartFile 是否为空，若为空则抛出异常
     *
     * @param multipartFile 文件对象
     */
    private static void checkMultipartFile(MultipartFile multipartFile) {
        if (ObjectUtil.isNull(multipartFile)) {
            throw new RuntimeException("MINIO文件对象为空");
        }
    }

    /**
     * 检查文件名称是否为空或空串，若为空或空串则抛出异常
     *
     * @param fileName 文件名称
     */
    private static void checkFileName(String fileName) {
        if (StrUtil.isEmpty(fileName)) {
            throw new RuntimeException("MINIO文件名为空或空串");
        }
    }

    /**
     * 检查文件上传目录名称是否为空或空串，若为空或空串则抛出异常
     *
     * @param dir 文件上传目录名称
     */
    private static void checkDir(String dir) {
        if (StrUtil.isEmpty(dir)) {
            throw new RuntimeException("MINIO文件上传目录名称为空或空串");
        }
    }

    /**
     * 检查MINIO桶是否存在，桶名为空或空串，或桶不存在均抛出异常
     *
     * @param bucketName MINIO桶
     */
    @SneakyThrows
    private static void checkBucket(String bucketName) {

        if (StrUtil.isEmpty(bucketName)) {
            throw new RuntimeException("MINIO桶名为空或空串");
        }

        // 校验数据桶是否存在
        if (!minioClient.bucketExists(bucketName)) {
            throw new RuntimeException("MinIO桶不存在");
        }
    }
}