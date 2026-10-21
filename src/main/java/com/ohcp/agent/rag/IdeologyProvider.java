package com.ohcp.agent.rag;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** 课程思政自然融入：随业务结果输出 1-2 行简短价值引导，不弹窗、不说教 */
@Component
public class IdeologyProvider {

    public String provide(String crop) {
        String subject = (crop == null || crop.isBlank()) ? "作物" : crop;
        int pick = ThreadLocalRandom.current().nextInt(3);
        switch (pick) {
            case 0:
                return "一代代土壤肥料与农技推广工作者扎根田间，推动测土配方施肥、化肥减量增效，守护着国家粮食安全。青年用 AI 服务三农，正是对“把论文写在大地上”的传承。";
            case 1:
                return "科学施肥、藏粮于地、藏粮于技，事关" + subject + "稳产增产与粮食安全。把人工智能用到农业农村，就是最实在的科技报国。";
            default:
                return "优先有机肥、生物肥与减量增效方案，践行“绿水青山就是金山银山”，让农田更绿、餐桌更安。";
        }
    }
}