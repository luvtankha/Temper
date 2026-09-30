package dev.temper.cerebro.ai;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import java.nio.file.*;
import java.io.*;
import java.security.*;
import java.util.*;

/** One reusable session and tokenizer; all tensors and outputs are request-scoped. */
public final class OnnxTextClassifier implements TextClassifier, AutoCloseable {
    public static final String MODEL_ID = "distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    public OnnxTextClassifier(Path directory) throws Exception {
        verify(directory.resolve("model.onnx"), "252cf7048af94a1599019fef35961b2bd3d6db13df0b0a4b032b92baeae31939");
        verify(directory.resolve("tokenizer.json"), "d241a60d5e8f04cc1b2b3e9ef7a4921b27bf526d9f6050ab90f9267a1f9e5c66");
        // DJL checks this supported property before its optional telemetry callback.
        System.setProperty("OPT_OUT_TRACKING", "true");
        tokenizer = HuggingFaceTokenizer.newInstance(directory.resolve("tokenizer.json"),
            Map.of("truncation", "true", "padding", "false", "maxLength", "128", "modelMaxLength", "512"));
        OrtSession created = null;
        try (var options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2);
            created = environment.createSession(directory.resolve("model.onnx").toString(), options);
            if (!created.getInputNames().equals(Set.of("input_ids", "attention_mask")))
                throw new IllegalStateException("Unsupported foundation model input signature");
            session = created;
        } catch (Exception error) {
            if (created != null) created.close();
            tokenizer.close();
            throw error;
        }
    }

    @Override public synchronized ClassificationResult classify(String text) {
        if (text == null || text.isBlank() || text.length() > 12000)
            throw new IllegalArgumentException("Text must contain 1–12000 characters");
        long started = System.nanoTime();
        var encoding = tokenizer.encode(text);
        try (var ids = OnnxTensor.createTensor(environment, new long[][] {encoding.getIds()});
             var mask = OnnxTensor.createTensor(environment, new long[][] {encoding.getAttentionMask()});
             var result = session.run(Map.of("input_ids", ids, "attention_mask", mask))) {
            float[] logits = ((float[][]) result.get(0).getValue())[0];
            if (logits.length != 2) throw new IllegalStateException("Invalid output labels");
            double max = Math.max(logits[0], logits[1]);
            double negative = Math.exp(logits[0] - max), positive = Math.exp(logits[1] - max);
            return new ClassificationResult(MODEL_ID, "MODEL", Map.of("negative", negative / (negative + positive),
                "positive", positive / (negative + positive)), encoding.getIds().length,
                (System.nanoTime() - started) / 1_000_000);
        } catch (OrtException error) {
            throw new IllegalStateException("Local classifier inference failed", error);
        }
    }

    private static void verify(Path file, String expected) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536]; int length;
            while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
        }
        if (!HexFormat.of().formatHex(digest.digest()).equals(expected))
            throw new IllegalStateException("Foundation model artifact checksum mismatch: " + file.getFileName());
    }

    @Override public synchronized void close() throws OrtException { session.close(); tokenizer.close(); }
}
