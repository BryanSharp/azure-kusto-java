// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

import com.microsoft.azure.kusto.data.ClientImpl;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import com.microsoft.azure.kusto.ingest.*;
import com.microsoft.azure.kusto.ingest.result.IngestionResult;
import com.microsoft.azure.kusto.ingest.source.FileSourceInfo;
import com.microsoft.azure.kusto.ingest.source.StreamSourceInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestAuth {
    public static void main(String[] args) {
        try {
            ConnectionStringBuilder csb = ConnectionStringBuilder.createWithUserPrompt("https://wdgeventstore.kusto.windows.net/");
            ClientImpl client = new ClientImpl(csb);

            KustoOperationResult results = client.execute("MMX", "LauncherEvents_RealTime | limit 1");
            KustoResultSetTable mainTableResult = results.getPrimaryResults();
            System.out.println(String.format("Kusto sent back %s rows.", mainTableResult.count()));

            // iterate values
            while (mainTableResult.next()) {
                List<Object> nextValue = mainTableResult.getCurrentRow();
                System.out.println(nextValue);
            }

            // in case we want to pass client request properties
//            ClientRequestProperties clientRequestProperties = new ClientRequestProperties();
//            clientRequestProperties.setTimeoutInMilliSec(TimeUnit.MINUTES.toMillis(1));
//
//            results = client.execute(System.getProperty("dbName"), System.getProperty("query"), clientRequestProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}