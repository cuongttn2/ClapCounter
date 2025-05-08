package com.sandipbhattacharya.clapcounter;

import java.util.List;

public interface SequenceListener {
    /** 
     * Gọi khi stop(): 
     *   segments.get(0) = số clap trước pause đầu tiên  
     *   segments.get(1) = số clap trước pause thứ hai, ...
     */
    void onSequence(List<Integer> segments);
    void onError(Exception e);
}