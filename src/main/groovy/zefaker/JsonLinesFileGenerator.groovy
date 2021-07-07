package zefaker

import com.github.javafaker.Faker
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

import java.util.concurrent.atomic.AtomicLong

class JsonLinesFileGenerator implements Generator {
    final Gson gson = new Gson()
    final AtomicLong generated = new AtomicLong(0)

    String name() {
        return "JSON Lines"
    }

    String fileExtension() {
        return ".jsonl"
    }

    void generate(Faker faker, Map<ColumnDef, Closure> columnDefs, int maxRows, Flushable output) {
        def buf = output as BufferedWriter
        try {
            while(generated.get() < maxRows) {
                def objNode = new JsonObject()
                
                columnDefs.each {
                    def col = it.getKey()
                    def fakerFunc = it.getValue()
                    def generatedValue = fakerFunc(faker)
                    if (generatedValue instanceof JsonElement) {
                        objNode.add(col.name, generatedValue)
                    } else {
                        // TODO: Check for supported data types for the addProperty call
                        if (generatedValue instanceof String || 
                                generatedValue instanceof Boolean ||
                                generatedValue instanceof Integer ||
                                generatedValue instanceof Long) {

                            objNode.addProperty(col.name, generatedValue)
                        } else {
                            objNode.addProperty(col.name, generatedValue.toString())
                        }
                    }
                }

                buf.write(String.format("%s", objNode))
                buf.newLine()
                generated.incrementAndGet();
                buf.flush()
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file", e)
        }
    }
}