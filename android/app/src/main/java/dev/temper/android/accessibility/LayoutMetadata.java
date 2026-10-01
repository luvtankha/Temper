package dev.temper.android.accessibility;

import android.content.Context;
import dev.temper.android.adapters.ScreenObservation;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Explicit debug export, structurally incapable of serializing node text. */
public final class LayoutMetadata {
    private LayoutMetadata(){}
    public static void export(Context context,ScreenObservation screen) throws Exception {
        JSONObject result=new JSONObject();result.put("schema",1);result.put("package",screen.packageName());result.put("viewport",rect(screen.viewport()));
        JSONArray nodes=new JSONArray();
        for(ScreenObservation.Node node:screen.nodes()){
            JSONObject item=new JSONObject();item.put("parent",node.parentIndex());item.put("id",node.resourceId());item.put("class",node.className());item.put("bounds",rect(node.bounds()));item.put("editable",node.editable());nodes.put(item);
        }
        result.put("nodes",nodes);Files.write(new File(context.getFilesDir(),"whatsapp-layout-metadata.json").toPath(),result.toString().getBytes(StandardCharsets.UTF_8));
    }
    public static void clear(Context context){new File(context.getFilesDir(),"whatsapp-layout-metadata.json").delete();}
    private static JSONArray rect(ScreenObservation.Bounds bounds){return new JSONArray().put(bounds.left()).put(bounds.top()).put(bounds.right()).put(bounds.bottom());}
}
