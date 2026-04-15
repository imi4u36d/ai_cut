package com.jiandou.api.upload.application;

import com.jiandou.api.upload.application.dto.UploadAssetResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传应用服务边界。
 */
public interface UploadApplicationService {

    /**
     * 上传文本。
     * @param file 待上传的文件
     * @return 处理结果
     */
    UploadAssetResponse uploadText(MultipartFile file);

    /**
     * 上传视频。
     * @param file 待上传的文件
     * @return 处理结果
     */
    UploadAssetResponse uploadVideo(MultipartFile file);
}
