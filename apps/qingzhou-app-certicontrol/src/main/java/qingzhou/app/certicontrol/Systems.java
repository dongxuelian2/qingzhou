package qingzhou.app.certicontrol;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import qingzhou.api.*;
import qingzhou.api.action.*;
import qingzhou.api.annotations.*;

@Model(code = "systems", menu = "control", icon = "Monitor", name = {"系统模型", "en:System models"})
public class Systems extends ModelBase implements Page, Show, Add, Update, Delete, Monitor {
    private final Map<String, Map<String,String>> records = new LinkedHashMap<>();
    private Path storage;
    private static final String[] INPUTS = {"name","a","b","c","k"};

    @ModelField(id=true, readonly=true, list=true, add=false, update=false, name={"编号","en:ID"})
    public String id;
    @ModelField(list=true, search=true, required=true, max_length=120, name={"系统名称","en:System name"})
    public String name;
    @ModelField(required=true, input_type=InputType.textarea, max_length=12000, name={"状态矩阵 A","en:State matrix A"}, info={"行用分号或换行、列用逗号或空格","en:Rows separated by semicolons or newlines, columns by commas or spaces"})
    public String a;
    @ModelField(required=true, input_type=InputType.textarea, max_length=12000, name={"输入矩阵 B","en:Input matrix B"})
    public String b;
    @ModelField(required=true, input_type=InputType.textarea, max_length=12000, name={"输出矩阵 C","en:Output matrix C"})
    public String c;
    @ModelField(input_type=InputType.textarea, max_length=12000, name={"状态反馈 K（可选）","en:State feedback K (optional)"}, info={"u=-Kx","en:u=-Kx"})
    public String k;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"状态维数","en:State dimension"})
    public String states;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"可控性","en:Controllability"})
    public String controllable;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"可观性","en:Observability"})
    public String observable;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"开环稳定性","en:Open-loop stability"})
    public String openStability;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"开环谱裕度","en:Open-loop spectral margin"})
    public String openMargin;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"闭环稳定性","en:Closed-loop stability"})
    public String closedStability;
    @ModelField(readonly=true, add=false, update=false, list=true, name={"闭环谱裕度","en:Closed-loop spectral margin"})
    public String closedMargin;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可控矩阵秩","en:Controllability rank"})
    public String controlRank;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可观矩阵秩","en:Observability rank"})
    public String observeRank;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可控性 PBH 检验","en:Controllability PBH test"})
    public String controlPbh;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可观性 PBH 检验","en:Observability PBH test"})
    public String observePbh;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可控矩阵奇异值","en:Controllability singular values"})
    public String controlSingularValues;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可观矩阵奇异值","en:Observability singular values"})
    public String observeSingularValues;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可控秩阈值","en:Controllability rank tolerance"})
    public String controlTolerance;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"可观秩阈值","en:Observability rank tolerance"})
    public String observeTolerance;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"开环极点","en:Open-loop poles"})
    public String openPoles;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"开环稳定性阈值","en:Open-loop stability tolerance"})
    public String openTolerance;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"开环Lyapunov 证书","en:Open-loop Lyapunov certificate"})
    public String openLyapunov;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"开环Lyapunov 方程残差","en:Open-loop Lyapunov residual"})
    public String openLyapunovResidual;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"开环P 最小特征值","en:Open-loop minimum eigenvalue of P"})
    public String openLyapunovMinEigenvalue;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"闭环极点","en:Closed-loop poles"})
    public String closedPoles;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"闭环稳定性阈值","en:Closed-loop stability tolerance"})
    public String closedTolerance;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"闭环Lyapunov 证书","en:Closed-loop Lyapunov certificate"})
    public String closedLyapunov;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"闭环Lyapunov 方程残差","en:Closed-loop Lyapunov residual"})
    public String closedLyapunovResidual;
    @ModelField(readonly=true, add=false, update=false, list=false, name={"闭环P 最小特征值","en:Closed-loop minimum eigenvalue of P"})
    public String closedLyapunovMinEigenvalue;

    @ModelField(field_type=FieldType.monitor, numeric=true, name={"开环谱裕度","en:Open-loop spectral margin"})
    public String monitoredOpenMargin;
    @ModelField(field_type=FieldType.monitor, numeric=true, name={"闭环谱裕度","en:Closed-loop spectral margin"})
    public String monitoredClosedMargin;

    @Override
    public synchronized void start() {
        storage = getAppContext().getRoot().toPath().resolve("certicontrol").resolve("systems.properties");
        if (!Files.exists(storage)) {
            try { persist(examples()); }
            catch (IOException e) { throw new IllegalStateException("Cannot initialize CertiControl examples",e); }
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(storage)) {
            properties.load(in);
            for (String key : properties.stringPropertyNames()) {
                int dot = key.indexOf('.');
                if (dot <= 0) throw new IOException("Invalid stored key");
                String id = key.substring(0,dot);
                records.computeIfAbsent(id, ignored -> new LinkedHashMap<>()).put(key.substring(dot+1),properties.getProperty(key));
            }
            for (Map.Entry<String,Map<String,String>> record : records.entrySet()) {
                record.setValue(validated(record.getKey(),record.getValue()));
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot load CertiControl systems",e); }
    }

    static Map<String,Map<String,String>> examples() {
        Map<String,Map<String,String>> values = new LinkedHashMap<>();
        values.put("example-01", example("example-01", "稳定振荡系统", "-1,-2;2,-1", "1;0", "1,0", ""));
        values.put("example-02", example("example-02", "双执行器状态反馈系统", "1,0;0,-2", "1,0;0,1", "1,0;0,1", "3,0;0,0"));
        values.put("example-03", example("example-03", "第一执行器失效系统", "1,0;0,-2", "0,0;0,1", "1,0;0,1", "3,0;0,0"));
        return values;
    }

    private static Map<String,String> example(String id,String name,String a,String b,String c,String k) {
        Map<String,String> input = new LinkedHashMap<>();
        input.put("name",name);
        input.put("a",a);
        input.put("b",b);
        input.put("c",c);
        input.put("k",k);
        return validated(id,input);
    }

    private void persist(Map<String,Map<String,String>> next) throws IOException {
        if (storage != null) {
            Files.createDirectories(storage.getParent());
            Properties properties = new Properties();
            next.forEach((key,value) -> { for (String field : INPUTS) if (value.containsKey(field)) properties.setProperty(key+"."+field,value.get(field)); });
            Path temporary = Files.createTempFile(storage.getParent(),"systems-", ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(temporary)) { properties.store(out,"CertiControl model registry"); }
                Files.move(temporary,storage,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        }
        records.clear();
        records.putAll(next);
    }

    private static Map<String,String> validated(String id, Map<String,String> input) {
        Map<String,String> value = new LinkedHashMap<>();
        for (String key : INPUTS) if (input.containsKey(key)) value.put(key,input.get(key));
        String name = value.get("name");
        if (name == null || name.trim().isEmpty() || name.length() > 120) throw new IllegalArgumentException("System name required, at most 120 characters");
        value.put("id",id);
        value.putAll(ControlAnalysis.analyze(new ControlAnalysis.SystemModel(value)));
        return value;
    }

    @Override public synchronized boolean contains(String id) { return records.containsKey(id); }

    @Override public synchronized Map<String,String> show(String id) {
        Map<String,String> record = records.get(id);
        if (record == null) throw new IllegalArgumentException("Unknown system: " + id);
        return new LinkedHashMap<>(record);
    }

    @Override public synchronized void add(Map<String,String> data) throws IOException {
        if (records.size() >= 200) throw new IllegalArgumentException("Maximum 200 systems");
        String id;
        do { id=UUID.randomUUID().toString().replace("-","").substring(0,16); } while (records.containsKey(id));
        Map<String,Map<String,String>> next = new LinkedHashMap<>(records);
        next.put(id,validated(id,data));
        persist(next);
    }

    @Override public synchronized void update(String id, Map<String,String> data) throws IOException {
        Map<String,String> changed = show(id);
        changed.putAll(data);
        Map<String,Map<String,String>> next = new LinkedHashMap<>(records);
        next.put(id,validated(id,changed));
        persist(next);
    }

    @Override public synchronized void delete(String id) throws IOException { delete(Collections.singletonList(id)); }
    @Override public synchronized void delete(List<String> ids) throws IOException {
        Map<String,Map<String,String>> next = new LinkedHashMap<>(records);
        for (String id : ids) if (!records.containsKey(id)) throw new IllegalArgumentException("Unknown system: "+id);
        ids.forEach(next::remove);
        persist(next);
    }

    private List<Map<String,String>> matching(Map<String,String> query) {
        String search = query.getOrDefault("name","").toLowerCase(Locale.ROOT);
        List<Map<String,String>> result = new ArrayList<>();
        for (Map<String,String> value : records.values()) if (value.get("name").toLowerCase(Locale.ROOT).contains(search)) result.add(value);
        return result;
    }

    @Override public synchronized int total(Map<String,String> query) { return matching(query).size(); }
    @Override public synchronized List<String[]> page(int pageNum,int pageSize,Map<String,String> query,String[] fields) {
        List<Map<String,String>> items = matching(query);
        List<String[]> rows = new ArrayList<>();
        int start = (int)Math.min(items.size(),Math.max(0L,((long)pageNum-1)*pageSize));
        for (int i=start;i<Math.min(items.size(),start+pageSize);i++) {
            String[] row = new String[fields.length];
            for (int j=0;j<fields.length;j++) row[j]=items.get(i).getOrDefault(fields[j],"");
            rows.add(row);
        }
        return rows;
    }

    @ModelAction(code="loadExamples", icon="Plus", list_head=true, name={"加载内置实例","en:Load examples"})
    public synchronized void loadExamples(Request request) throws IOException {
        Map<String,Map<String,String>> next = new LinkedHashMap<>(records);
        for (Map.Entry<String,Map<String,String>> example : examples().entrySet()) {
            next.putIfAbsent(example.getKey(), example.getValue());
        }
        if (next.size() > 200) throw new IllegalArgumentException("Maximum 200 systems");
        persist(next);
        request.getResponse().message("内置实例已加载 / Examples loaded");
    }

    @ModelAction(code="diagnose", icon="Check", list=true, show=true, popup=true, name={"重新诊断","en:Run diagnostics"})
    public void diagnose(Request request) {
        Map<String,String> report = ControlAnalysis.analyze(new ControlAnalysis.SystemModel(show(request.getId())));
        request.getResponse().data(report).message("Diagnostics completed");
    }

    @ModelAction(code="faults", icon="Warning", list=true, show=true, popup=true, name={"单通道故障分析","en:Single-channel fault analysis"})
    public void faults(Request request) {
        request.getResponse().data(ControlAnalysis.faults(new ControlAnalysis.SystemModel(show(request.getId())))).message("Fault analysis completed");
    }

    @Override public synchronized Map<String,String> monitor(String id) {
        Map<String,String> result = new LinkedHashMap<>();
        if (!contains(id)) return result;
        Map<String,String> values = show(id);
        result.put("monitoredOpenMargin",values.get("openMargin"));
        if (values.containsKey("closedMargin")) result.put("monitoredClosedMargin",values.get("closedMargin"));
        return result;
    }
}
