package entity;

import lombok.Data;

@Data
public class Reply {
    /**
     * 消息类型
     */
    private String type;
    /**
     * 异常消息
     */
    private String errorMessage;
    /**
     * 提示消息
     */
    private String hintMessage;
    /**
     * 成功消息
     */
    private String successMessage;
    /**
     * 帧
     */
    private String frame;
}
