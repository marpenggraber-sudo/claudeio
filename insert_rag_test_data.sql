-- RAG 测试数据插入脚本
-- 用于在小程序中测试 RAG 功能

USE music_agent;

-- 清空现有知识库（可选）
-- DELETE FROM music_knowledge;

-- 1. 艺术家知识：周杰伦
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    '周杰伦',
    '周杰伦是华语流行音乐天王，出生于1979年1月18日，台湾著名歌手、词曲创作人、音乐制作人。他擅长中国风、R&B、流行摇滚等多种风格。代表作有《青花瓷》《稻香》《七里香》《菊花台》《东风破》《夜曲》《简单爱》等。他的音乐融合了古典与流行元素，歌词富有诗意，被誉为"亚洲流行天王"。周杰伦还涉足电影、导演等领域，是华语乐坛最具影响力的人物之一。',
    'ARTIST',
    '周杰伦,Jay Chou,周董,杰伦,中国风,R&B,青花瓷,稻香',
    NOW(),
    NOW()
);

-- 2. 音乐风格知识：民谣
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    '民谣音乐',
    '民谣音乐（Folk Music）是一种注重歌词和情感表达的音乐风格，通常使用吉他、口琴、手鼓等简单乐器。民谣强调真实的生活感受和故事性，歌词往往贴近日常生活，充满人文关怀。民谣音乐起源于民间，后来发展成为独立音乐的重要分支。中国民谣代表歌手有赵雷、宋冬野、马頔、陈粒、好妹妹乐队等。经典民谣作品包括《成都》《董小姐》《南山南》《理想三旬》等。民谣强调真诚、质朴，是年轻人喜爱的音乐类型之一。',
    'GENRE',
    '民谣,folk,吉他,赵雷,宋冬野,马頔,陈粒,成都,董小姐,南山南',
    NOW(),
    NOW()
);

-- 3. 音乐理论知识：中国风
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    '中国风音乐',
    '中国风音乐是融合中国传统音乐元素和现代流行音乐的风格，也称为"古风"。其特点包括：使用古筝、琵琶、二胡、笛子等传统民族乐器；歌词常引用古诗词、历史典故、成语；旋律带有五声音阶特色；意境唯美古典。中国风音乐的开创者是周杰伦，他的《东风破》《青花瓷》《菊花台》《发如雪》等作品开创了中国风流行音乐的先河。其他代表歌手还有许嵩、汪苏泷、李玉刚等。中国风音乐将传统文化与现代音乐完美结合，深受年轻人喜爱。',
    'GENRE',
    '中国风,古风,周杰伦,青花瓷,东风破,古筝,琵琶,二胡,古诗词,许嵩',
    NOW(),
    NOW()
);

-- 4. 音乐常识知识：说唱
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    '说唱音乐',
    '说唱（Rap/Hip-Hop）是一种强调节奏和韵脚的音乐形式，歌手通过快速、有韵律的念白（而非传统演唱）表达内容。说唱起源于20世纪70年代美国黑人文化，强调个人表达、社会批判和街头文化。说唱的四大元素包括：MC（说唱）、DJ（打碟）、Breaking（霹雳舞）、Graffiti（涂鸦）。中国说唱代表有GAI、PG One、Vava、艾热、孙八一等。近年来通过《中国有嘻哈》《说唱新世代》等综艺节目，说唱在中国迅速流行，成为年轻人最喜爱的音乐类型之一。',
    'GENRE',
    '说唱,Rap,Hip-Hop,嘻哈,GAI,PG One,Vava,中国有嘻哈,MC,DJ',
    NOW(),
    NOW()
);

-- 5. 艺术家知识：Taylor Swift
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    'Taylor Swift',
    'Taylor Swift（泰勒·斯威夫特）是美国著名流行歌手、词曲创作人，出生于1989年12月13日。她是格莱美奖得主，被誉为"美国甜心"。Taylor Swift 最初以乡村音乐出道，后来转型为流行音乐。代表作有《Love Story》《You Belong With Me》《Shake It Off》《Blank Space》《Look What You Made Me Do》等。她擅长将个人情感经历融入歌曲创作，歌词细腻动人。Taylor Swift 是全球最畅销的音乐艺术家之一，拥有数亿粉丝，对流行文化影响深远。',
    'ARTIST',
    'Taylor Swift,泰勒斯威夫特,霉霉,Love Story,Shake It Off,流行音乐,美国歌手',
    NOW(),
    NOW()
);

-- 6. 音乐理论知识：R&B
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    'R&B音乐',
    'R&B（Rhythm and Blues，节奏布鲁斯）是一种融合了爵士、福音和布鲁斯元素的流行音乐风格。R&B 起源于20世纪40年代的美国黑人音乐，特点是节奏感强、旋律优美、情感丰富。现代 R&B 融入了电子音乐、流行、嘻哈等元素，更加多元化。华语乐坛的 R&B 代表有周杰伦、陶喆、王力宏、潘玮柏等。国际 R&B 代表有 Usher、Chris Brown、Beyoncé、The Weeknd 等。R&B 强调律动感和情感表达，是流行音乐中非常重要的一个分支。',
    'GENRE',
    'R&B,节奏布鲁斯,RnB,周杰伦,陶喆,王力宏,Usher,Beyonce,节奏,律动',
    NOW(),
    NOW()
);

-- 7. 音乐常识知识：电子音乐
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    '电子音乐',
    '电子音乐（Electronic Music）是使用电子乐器和数字技术制作的音乐。包括多种子风格：EDM（电子舞曲）、House、Techno、Dubstep、Trance、Drum and Bass 等。电子音乐特点是节奏强烈、低音厚重、适合舞蹈和健身。著名电子音乐人有 Avicii、The Chainsmokers、Marshmello、David Guetta、Martin Garrix 等。电子音乐常用于夜店、音乐节、健身房等场景，具有强烈的律动感和能量感。近年来电子音乐与流行音乐融合，成为主流音乐的重要组成部分。',
    'GENRE',
    '电子音乐,EDM,电子舞曲,House,Techno,Dubstep,Avicii,Marshmello,DJ,电音',
    NOW(),
    NOW()
);

-- 8. 音乐推荐原则
INSERT INTO music_knowledge (title, content, knowledge_type, related_keywords, created_at, updated_at)
VALUES (
    '音乐推荐原则',
    '好的音乐推荐应该遵循以下原则：1. 了解用户偏好 - 根据用户的历史播放记录、收藏、点赞等行为分析用户喜好；2. 场景匹配 - 根据使用场景推荐合适的音乐，如健身推荐节奏强烈的歌曲，睡觉推荐轻音乐；3. 风格多样性 - 在用户喜欢的风格基础上，适度推荐相似但不完全相同的音乐，帮助用户发现新歌；4. 时效性 - 结合热门榜单、新歌推荐，保持推荐的新鲜度；5. 情感共鸣 - 推荐歌词和旋律能引起用户情感共鸣的音乐。好的推荐系统应该兼顾精准度和探索性，既满足用户期望又带来惊喜。',
    'THEORY',
    '推荐系统,音乐推荐,个性化,场景,用户偏好,算法',
    NOW(),
    NOW()
);

-- 查询插入结果
SELECT
    id,
    title,
    knowledge_type,
    SUBSTRING(content, 1, 50) AS content_preview,
    related_keywords,
    created_at
FROM music_knowledge
ORDER BY id DESC
LIMIT 10;

-- 显示总数
SELECT COUNT(*) AS total_knowledge FROM music_knowledge;
