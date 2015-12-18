package com.chinamobile.cmti.faceclassification;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

import com.chinamobile.faceClassification.server.ActionsRequestBuilders;
import com.chinamobile.faceClassification.server.FaceClassification;
import com.chinamobile.faceClassification.server.FaceImage;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

public class FaceClassificationService {
    // Create an HttpClient and wrap it in an abstraction layer
    private static final HttpClientFactory http = new HttpClientFactory();
    private static final Client r2Client = new TransportClientAdapter(
            http.getClient(Collections.<String, String>emptyMap()));
    private static final String BASE_URL = "http://52.53.254.134:6666/";
    //    private static final String BASE_URL = "http://localhost:6666/";
    private static RestClient restClient = new RestClient(r2Client, BASE_URL);

    private static ActionsRequestBuilders actionsRequestBuilders = new ActionsRequestBuilders();

    public static FaceClassification classifyImage(String fileName, Bitmap bitmap){
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, 47, 57);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        FaceImage faceImage = new FaceImage().setImageName(getBaseName(fileName)).setImageContent(ByteString.copy(stream.toByteArray()));

        ActionRequest<FaceClassification> actionRequest = actionsRequestBuilders.actionClassifyPhoto().faceImageParam(faceImage).build();

        try{
            ResponseFuture<FaceClassification> responseFuture = restClient.sendRequest(actionRequest);
            Response<FaceClassification> response = responseFuture.getResponse();

            FaceClassification faceClassification = response.getEntity();
            System.out.println("\nclassifyImage returns: " + (faceClassification == null ? "null" : faceClassification));

            return faceClassification;
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getBaseName(String fileName){
        if(fileName == null)
            return null;

        String[] parts = fileName.split("/");
        return parts[parts.length - 1];
    }

    private static byte[] readFromFile(String fileName) {
        try {
            File file = new File(fileName);
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();

            return fileData;
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }
}
