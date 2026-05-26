package org.example.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import org.example.dto.AgentReply;
import org.example.tools.MusicTools;

@AiService(tools = "musicTools")
public interface MusicAgent {

    @SystemMessage("你是一个专业的音乐助手。你必须优先使用工具完成搜索、播放和缓存歌曲的任务。用户说‘播放第二首’时，要从缓存推荐列表里取歌。回答要简洁、自然、中文优先。")
    AgentReply chat(@UserMessage String message, Long userId);
}
