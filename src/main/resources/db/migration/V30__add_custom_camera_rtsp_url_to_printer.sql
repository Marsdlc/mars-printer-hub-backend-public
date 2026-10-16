-- 添加自定义摄像头RTSP路径字段
ALTER TABLE printers ADD COLUMN custom_camera_rtsp_url VARCHAR(500) NULL COMMENT '自定义摄像头RTSP路径';
