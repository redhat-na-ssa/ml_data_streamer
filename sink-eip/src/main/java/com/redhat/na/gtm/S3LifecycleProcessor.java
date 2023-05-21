package com.redhat.na.gtm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.redhat.na.gtm.ml.Util;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.quarkiverse.minio.client.MinioQualifier;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class S3LifecycleProcessor {

    private static Logger log = Logger.getLogger(S3LifecycleProcessor.class);

    private Random random = new Random();

    @Inject
    @MinioQualifier("rht")
    MinioClient mClient;

    @ConfigProperty(name = "com.rht.na.gtm.s3.printMinIOresponseHeaders", defaultValue="False")
    protected boolean printResponseHeaders;

    @ConfigProperty(name = "com.rht.na.gtm.s3.bucket.name")
    String bucketName;

    // https://min.io/docs/minio/linux/administration/concepts.html#can-i-organize-objects-in-a-folder-structure-within-buckets
    @ConfigProperty(name = "com.rht.na.gtm.s3.minIObucketPrefix")
    protected String minIOBucketPrefix;
    
    @ConfigProperty(name = "com.rht.na.gtm.s3.minIOobjectTags")
    protected String minIOobjectTags;

    @PostConstruct
    void start() {
        try {
            boolean bucketExists = mClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if(!bucketExists) {
                log.infov("About to make new bucket: {0}", bucketName);
                mClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e1) {

            e1.printStackTrace();
        }

    }

    public void postBody(Exchange e){
        String body = (String)e.getIn().getBody();
        //log.info("postBody() posting to bucket :"+bucketName+" ; byte[] length = "+body.getBytes().length);

        Map<String, String> tags = new HashMap<String, String>();
        String[] tagsArray = minIOobjectTags.split(",");
        for(String pairs : tagsArray){
            String[] pair = pairs.split(":");
            tags.put(pair[0], pair[1]);
        }

        String objectName = minIOBucketPrefix+"/";
        byte[] fHeaderBytes = (byte[])e.getIn().getHeader(Util.FILE_NAME_HEADER);
        if(fHeaderBytes == null){
            objectName = objectName+"com.redhat.na.gtm_"+random.nextInt(10000);
        }else{
            objectName = objectName + (new String(fHeaderBytes));
        }
        log.infov("uploading object to {0} with following # of tags: {1}", bucketName, tags.size());
        ObjectWriteResponse owResponse = null;
        InputStream iStream = null;
        try {

            // https://min.io/docs/minio/linux/developers/java/API.html#putobject-putobjectargs-args
            // Upload input stream with headers and user metadata.
            Map<String, String> headers = new HashMap<>();
            Map<String, String> userMetadata = new HashMap<>();
            iStream = new ByteArrayInputStream(body.getBytes());
            owResponse = mClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(iStream, body.getBytes().length, -1)
                .headers(headers)
                .userMetadata(userMetadata)
                .tags(tags)
                .build());
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e1) {
            e1.printStackTrace();
        }finally{
            if(iStream != null)
                try {
                    iStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }

        if(printResponseHeaders && (owResponse != null)){
            Set<String> hNames = owResponse.headers().names();
            for(String key : hNames){
                log.infov("return header = {0} , {1}", key, owResponse.headers().get(key));
            }
        }

    }
    
}
