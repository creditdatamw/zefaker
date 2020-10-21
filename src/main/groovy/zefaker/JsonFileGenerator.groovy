package zefaker

import com.github.javafaker.Faker
import com.google.gson.*;

import java.nio.file.Paths
import java.nio.file.Files
import java.io.BufferedWriter
import java.util.stream.Collectors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

class JsonFileGenerator implements Runnable {
    def columnDefs
    def filePath
    def maxRows = 10
    final CountDownLatch latch
    final Faker faker
    final AtomicLong generated = new AtomicLong(0)
    final Gson gson = new Gson()

    JsonFileGenerator(faker, filePath, columnDefs, maxRows, latch) {
        this.faker = faker
        this.filePath = filePath
        this.columnDefs = columnDefs
        this.latch = latch
        this.maxRows = maxRows
    }

    void run() {
        def jsonArr = new JsonArray()
         try {
            while(generated.get() < maxRows) {
                def jsonNode = new JsonObject()
                
                columnDefs.each {
                    def col = it.getKey()
                    def fakerFunc = it.getValue()
                    def generatedValue = fakerFunc(faker)
                    if (generatedValue instanceof JsonElement) {
                        jsonNode.add(col.name, generatedValue)
                    } else {
                        // TODO: Check for supported data types for the addProperty call
                        jsonNode.addProperty(col.name, generatedValue)
                    }
                }

                jsonArr.add(jsonNode)

                generated.incrementAndGet();
            }
            def fw = Files.newBufferedWriter(filePath)
            gson.toJson(jsonArr, fw)
            fw.close()
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file", e)
        } finally {
            latch.countDown()
        }
    }
}