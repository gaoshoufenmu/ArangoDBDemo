package utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

import java.util.List;

public class NLP {
    private static Segment seg = HanLP.newSegment().enableAllNamedEntityRecognize(true);

    public static String findPersonName(String name) {
        if(name==null || "".equals(name)) return null;

        List<Term> terms = seg.seg(name);
        for(Term t : terms) {
            if (t.nature == Nature.nr
                    || t.nature == Nature.nr1
                    || t.nature == Nature.nr2
            || t.nature == Nature.nrf
            || t.nature == Nature.nrj) return t.word;
        }
        return null;
    }

    public static String findOrgName(String name) {
        if(name==null || "".equals(name)) return null;

        List<Term> terms = seg.seg(name);
        for (Term t : terms) {
            if (t.nature.ordinal() >= Nature.nt.ordinal()
                && t.nature.ordinal() <= Nature.nth.ordinal()) {
                return t.word;
            }
        }
        return null;
    }
}
