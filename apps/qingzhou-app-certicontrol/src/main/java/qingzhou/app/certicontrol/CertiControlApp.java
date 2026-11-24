package qingzhou.app.certicontrol;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.annotations.App;
import qingzhou.api.annotations.Menu;

@Menu(code = "control", icon = "Monitor", name = {"控制系统", "en:Control systems"}, order = 1)
@App(roles = "admin", icon = "DataBoard", name = {"CertiControl 控制系统诊断", "en:CertiControl"},
        info = {"连续时间线性系统的可控性、可观性、稳定性与故障分析", "en:Continuous-time LTI diagnostics"})
public class CertiControlApp implements QingzhouApp {
    @Override
    public void start(AppContext context) {
    }
}
