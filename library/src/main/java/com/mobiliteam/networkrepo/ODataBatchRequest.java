package com.mobiliteam.networkrepo;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by swapnilnandgave on 14/12/18.
 */

public final class ODataBatchRequest {

    public static final String BOUNDARY_BY = "--";

    public static final String NEWLINE = "\r\n";

    public static final String HTTP_APP_TYPE = "Content-Type: application/http";

    public static final String HTTP_JSON_TYPE = "Content-Type: application/json";

    public static final String HTTP_TRANS_ENCODING = "Content-Transfer-Encoding: binary";

    private List<BatchRequestItem> batchRequestItems = new ArrayList<>();

    private String boundaryName = "";

    private ODataBatchRequest(String boundaryName, List<BatchRequestItem> batchRequestItems) {
        this.boundaryName = boundaryName;
        this.batchRequestItems = batchRequestItems;
    }

    public List<BatchRequestItem> getBatchRequestItems() {
        return batchRequestItems;
    }

    public String getBoundaryName() {
        return boundaryName;
    }

    public String rawBody() {
        StringBuilder stringBuilder = new StringBuilder();
        for (BatchRequestItem batchRequestItem : batchRequestItems) {
            stringBuilder.append(startBoundary()).append(NEWLINE);
            stringBuilder.append(getDefaultPartHeader()).append(NEWLINE);
            stringBuilder.append(NEWLINE);
            stringBuilder.append(batchRequestItem.getReqLine());
            stringBuilder.append(NEWLINE);
            String contentLine = batchRequestItem.getContentLine();
            if (contentLine != null) {
                stringBuilder.append(batchRequestItem.getContentLine());
                stringBuilder.append(NEWLINE);
            }
            stringBuilder.append(NEWLINE);
        }
        stringBuilder.append(endBoundary());
        return stringBuilder.toString();
    }

    public List<NetworkResponse> parseBatchResponse(final String httpBody) {
        List<IndexWrapper> batchBlockIndexes = new ArrayList<>();
        List<String> batchBlocks = new ArrayList<>();

        List<NetworkResponse> networkResponses = new ArrayList<>();

        final String boundary = (httpBody.trim().split(ODataBatchRequest.NEWLINE)[0]).trim().replaceAll(ODataBatchRequest.BOUNDARY_BY, "");
        // Remove End Tag From Body
        final String content = httpBody.replaceAll(ODataBatchRequest.BOUNDARY_BY + boundary + ODataBatchRequest.BOUNDARY_BY, "").trim();
        final String regex = "" + ODataBatchRequest.BOUNDARY_BY + boundary + "";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int end = matcher.end();
            int start = matcher.start();
            batchBlockIndexes.add(new IndexWrapper(start, end));
        }
        for (int i = 0; i < batchBlockIndexes.size(); i++) {
            final IndexWrapper firstIndex = batchBlockIndexes.get(i);
            int endIndex = 0;
            if ((i + 1) >= batchBlockIndexes.size()) {
                endIndex = content.length();
            } else {
                final IndexWrapper nextIndex = batchBlockIndexes.get(i + 1);
                endIndex = nextIndex.getStart();
            }
            final String block = content.substring(firstIndex.getEnd(), endIndex);
            batchBlocks.add(block.trim());
        }
        if (this.getBatchRequestItems().size() == batchBlocks.size()) {
            for (int i = 0; i < batchBlocks.size(); i++) {
                NetworkResponse networkResponse = new NetworkResponse();
                String batchBlock = batchBlocks.get(i).replaceAll(ODataBatchRequest.HTTP_APP_TYPE, "").trim();
                batchBlock = batchBlock.replaceAll(ODataBatchRequest.HTTP_TRANS_ENCODING, "").trim();
                BatchRequestItem requestItem = this.getBatchRequestItems().get(i);

                final String[] httpLines = batchBlock.split(NEWLINE, 2);
                final String[] httpContext = httpLines[0].trim().split(" ", 3);
                networkResponse.setStatusCode(Integer.parseInt(httpContext[1]));
                networkResponse.setSuccess(NetworkRepository.validate(requestItem.httpMethod, networkResponse.getStatusCode()));
                try {
                    if (httpLines.length > 1) {
                        final String responsePart = httpLines[1].trim();
                        networkResponse.setResponse(responsePart);
                        if (responsePart.contains(HTTP_JSON_TYPE)) {
                            final int firstIndex = responsePart.indexOf("{");
                            final int lastIndex = responsePart.lastIndexOf("}");
                            networkResponse.setResponse(responsePart.substring(firstIndex, lastIndex + 1));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                networkResponses.add(networkResponse);
            }
        } else {
            Log.e("ODataBatchRequest", "Requests and Responses are not getting match");
        }
        return networkResponses;
    }

    private String startBoundary() {
        return ODataBatchRequest.BOUNDARY_BY + boundaryName;
    }

    private String endBoundary() {
        return ODataBatchRequest.BOUNDARY_BY + boundaryName + ODataBatchRequest.BOUNDARY_BY;
    }

    private String getDefaultPartHeader() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HTTP_APP_TYPE + NEWLINE + HTTP_TRANS_ENCODING);
        return stringBuilder.toString();
    }

    public static final class Builder {

        private List<BatchRequestItem> batchRequestItems = new ArrayList<>();

        private String boundaryName = "";

        public Builder() {
            this(UUID.randomUUID().toString());
        }

        public Builder(String boundaryName) {
            this.boundaryName = boundaryName;
        }

        public Builder addRequest(NetworkRepository.NetworkHttpMethod httpMethod, String urlPart) {
            batchRequestItems.add(new BatchRequestItem(httpMethod, urlPart, null));
            return this;
        }

        public Builder addRequest(NetworkRepository.NetworkHttpMethod httpMethod, String urlPart, String partBody) {
            batchRequestItems.add(new BatchRequestItem(httpMethod, urlPart, partBody));
            return this;
        }

        public Builder addRequest(NetworkRepository.NetworkHttpMethod httpMethod, String urlPart, String partBody, String httpVersion) {
            batchRequestItems.add(new BatchRequestItem(httpMethod, urlPart, partBody, httpVersion));
            return this;
        }

        public ODataBatchRequest build() {
            if (batchRequestItems.size() > 0) {
                return new ODataBatchRequest(boundaryName, batchRequestItems);
            } else {
                throw new RuntimeException("Batch request contain no requests");
            }
        }

    }

    private static final class BatchRequestItem {

        private NetworkRepository.NetworkHttpMethod httpMethod = NetworkRepository.NetworkHttpMethod.GET;
        private String httpVersion = "HTTP/1.1";
        private String urlPart = null;
        private String partBody = null;

        public BatchRequestItem(NetworkRepository.NetworkHttpMethod httpMethod, String urlPart, String partBody) {
            this(httpMethod, urlPart, partBody, "HTTP/1.1");
        }

        public BatchRequestItem(NetworkRepository.NetworkHttpMethod httpMethod, String urlPart, String partBody, String httpVersion) {
            this.httpMethod = httpMethod;
            this.urlPart = urlPart;
            this.httpVersion = httpVersion;
            this.partBody = partBody;
            if (httpMethod == null) {
                throw new RuntimeException("HTTP Method should not be null");
            }
            if (urlPart == null) {
                throw new RuntimeException("URL should not be null");
            }
            if (httpVersion == null) {
                throw new RuntimeException("HTTP Version should not be null");
            }
        }

        public String getReqLine() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(httpMethod.name()).append(" ");
            stringBuilder.append(urlPart).append(" ");
            stringBuilder.append(httpVersion);
            return stringBuilder.toString();
        }

        public String getContentLine() {
            if (partBody != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Content-Type: application/json").append(NEWLINE);
                stringBuilder.append("Content-Length: " + partBody.length()).append(NEWLINE);
                stringBuilder.append(NEWLINE);
                stringBuilder.append(partBody);
                return stringBuilder.toString();
            } else {
                return null;
            }
        }

    }

}
