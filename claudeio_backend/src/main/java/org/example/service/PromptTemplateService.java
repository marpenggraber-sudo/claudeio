package org.example.service;

import org.example.dto.IntentResult;
import org.example.dto.IntentType;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    public String buildSystemPrompt(IntentResult intent) {
        String base = "你是一个专业的音乐助手。回答要简洁、自然、中文优先。";
        if (intent == null) {
            return base;
        }
        return switch (intent.type()) {
            case GREETING -> base + " 用户在打招呼，请礼貌回应，并主动邀请他点歌或推荐。";
            case SEARCH -> base + " 用户想搜索歌曲，请优先根据关键词给出歌曲列表。";
            case PLAY_BY_INDEX -> base + " 用户想播放列表里的第几首，请依据缓存列表和索引处理。";
            case PLAY_BY_KEYWORD -> base + " 用户想播放一首歌，请根据歌名/歌手理解后给出列表并可继续播放。";
            case MUSIC_MEMORY -> base + " 用户在询问自己的听歌历史或音乐偏好，请只基于系统提供的记录回答，不要搜索或播放。";
            case KNOWLEDGE -> base + " 用户在询问音乐知识（艺术家、风格、乐理等），请基于知识库提供专业、准确的回答，不要搜索或播放歌曲。";
            case LOGOUT -> base + " 用户想退出登录，请简短确认。";
            case SWITCH_ACCOUNT -> base + " 用户想切换账号，请引导输入账号密码或确认切换结果。";
            case PLAYER_CONTROL -> base + " 用户想控制播放器（暂停、继续、下一首、上一首、调整音量），请简短确认操作。";
            case CHAT -> base + " 用户是在日常聊天，请自然回应，不要强行搜索或播放。";
            case UNKNOWN -> base + " 用户意图不明确，请先澄清需求。";
        };
    }
}
