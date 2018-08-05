package com.yun9.service.tax.core.utils;

import com.yun9.service.tax.core.exception.ServiceTaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.util.UUID;

public abstract class FileUtil {
    public static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private String path;
    public static String importFile(String url,String fileUrl) {
        String imgPath = "";
        InputStream inputStream = null;
        File file = new File(fileUrl);
        String name = file.getName();
        logger.debug("开始保存文件：{}", file.getName());
        try {
            inputStream = new FileInputStream(file);
            byte[] data = readInputStream(inputStream);
            imgPath = url+"\\/file";
            File imageFile = new File(imgPath);
            if (!imageFile.exists() && !imageFile.isDirectory()) {
                imageFile.mkdirs();
            }
            name = name.replaceAll(".xlsx",".xls");
            String saveFile = imageFile + "\\" + LocalDate.now() + UUID.randomUUID() + name;
            FileOutputStream outStream = new FileOutputStream(saveFile);
            outStream.write(data);
            //关闭输出流
            outStream.close();
            logger.debug("保存文件成功,地址为：{}", saveFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR ,e);
        }
        return imgPath;
    }
    public static String importFile(String url,byte[] data ,String name) {

        String imgPath = "";
        InputStream inputStream = null;
        String saveFile = "";
        try {
            imgPath = url+"\\/file";
            File imageFile = new File(imgPath);
            logger.info("创建文件夹{}开始",imgPath);
            if (!imageFile.exists() && !imageFile.isDirectory()) {
                logger.info("创建文件夹{}成功",imgPath);
                imageFile.mkdirs();
            }
             saveFile = imageFile + "/" + LocalDate.now() + UUID.randomUUID() + name;
            FileOutputStream outStream = new FileOutputStream(saveFile);
            outStream.write(data);
            //关闭输出流
            outStream.close();
            logger.info("保存文件成功,地址为：{}", saveFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw ServiceTaxException.build(ServiceTaxException.Codes.EXPORT_EXCEL_ERROR ,e);
        }
        return saveFile;
    }


    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }
}
