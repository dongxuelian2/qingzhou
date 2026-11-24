package qingzhou.app.certicontrol;

import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ControlAnalysisTest {
    private Map<String,String> model(String a,String b,String c,String k) {
        Map<String,String> m = new LinkedHashMap<>();
        m.put("name","test system"); m.put("a",a); m.put("b",b); m.put("c",c); m.put("k",k);
        return m;
    }

    @Test public void stableOscillator() {
        Map<String,String> r=ControlAnalysis.analyze(new ControlAnalysis.SystemModel(model("-1,-2;2,-1","1;0","1,0","")));
        Assert.assertEquals(r.get("controllable"),"PASS");
        Assert.assertEquals(r.get("observable"),"PASS");
        Assert.assertEquals(r.get("openStability"),"HURWITZ");
        Assert.assertEquals(Double.parseDouble(r.get("openMargin")),1.0,1e-10);
        Assert.assertEquals(r.get("openLyapunov"),"PASS");
    }

    @Test public void defectiveSystemAndFaults() {
        ControlAnalysis.SystemModel s=new ControlAnalysis.SystemModel(model("1,0;0,-2","1,0;0,1","1,0;0,1","3,0;0,0"));
        Map<String,String> r=ControlAnalysis.analyze(s);
        Assert.assertEquals(r.get("openStability"),"UNSTABLE");
        Assert.assertEquals(r.get("closedStability"),"HURWITZ");
        Map<String,String> faults=ControlAnalysis.faults(s);
        Assert.assertEquals(faults.get("actuator 1 controllability"),"FAIL");
        Assert.assertEquals(faults.get("actuator 1 closed-loop stability"),"UNSTABLE");
        Assert.assertEquals(faults.get("sensor 2 observability"),"FAIL");
    }

    @Test public void missingModeAndBoundary() {
        Map<String,String> r=ControlAnalysis.analyze(new ControlAnalysis.SystemModel(model("0,0;0,-1","0;1","0,1","")));
        Assert.assertEquals(r.get("controllable"),"FAIL");
        Assert.assertEquals(r.get("observable"),"FAIL");
        Assert.assertEquals(r.get("openStability"),"BOUNDARY_OR_INCONCLUSIVE");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void invalidDimensions() {
        new ControlAnalysis.SystemModel(model("1,2;3,4","1","1,0",""));
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void rejectsNonfinite() { ControlAnalysis.parse("NaN"); }

    @Test public void builtInExamples() {
        Map<String,Map<String,String>> examples=Systems.examples();
        Assert.assertEquals(examples.size(),3);
        Assert.assertEquals(examples.get("example-01").get("openStability"),"HURWITZ");
        Assert.assertEquals(examples.get("example-02").get("closedStability"),"HURWITZ");
        Assert.assertEquals(examples.get("example-03").get("closedStability"),"UNSTABLE");
        Assert.assertEquals(examples.get("example-03").get("controllable"),"FAIL");
        Assert.assertEquals(Double.parseDouble(examples.get("example-02").get("closedMargin")),2.0,1e-10);
        Assert.assertEquals(Double.parseDouble(examples.get("example-03").get("closedMargin")),-1.0,1e-10);
    }

    @Test public void registryLifecycle() throws Exception {
        Systems registry=new Systems();
        registry.add(model("-1","1","1",""));
        List<String[]> rows=registry.page(1,10,Collections.emptyMap(),new String[]{"id","openStability"});
        Assert.assertEquals(rows.size(),1);
        String id=rows.get(0)[0];
        Assert.assertEquals(rows.get(0)[1],"HURWITZ");
        registry.update(id,Collections.singletonMap("a","1"));
        Assert.assertEquals(registry.show(id).get("openStability"),"UNSTABLE");
        Assert.assertEquals(registry.monitor(id).get("monitoredOpenMargin"),"-1.0");
        registry.delete(id);
        Assert.assertEquals(registry.total(Collections.emptyMap()),0);
    }
}
