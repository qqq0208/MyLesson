package com.md.vo;

@Schema(name = "集次全查VO")
@Data  
public class EpisodeSimpleListVO implements Serializable {  
    @Schema(description = "主键")  
    private Long id;  
    @Schema(description = "标题")  
    private String title;  
}