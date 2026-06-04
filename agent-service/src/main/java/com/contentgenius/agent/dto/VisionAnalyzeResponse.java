package com.contentgenius.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisionAnalyzeResponse {

    private String objectName;

    /** 预签名地址，前端可继续展示原图 */
    private String imageUrl;

    /** 模型输出的风格/结构/语气说明，供用户仿写 */
    private String styleHint;
}
