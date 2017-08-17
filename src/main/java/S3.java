import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.testng.SkipException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SSECustomerKey;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.StringUtils;

public class S3 {
	
	static Properties prop = new Properties();
	InputStream input = null;
	
	AmazonS3 svc = getCLI();
	
	public AmazonS3 getCLI() {
		
		try {
			input = new FileInputStream("config.properties");
			try {
				prop.load(input);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String endpoint = prop.getProperty("endpoint");
		String accessKey = prop.getProperty("access_key");
		String secretKey = prop.getProperty("access_secret");
		Boolean issecure = Boolean.parseBoolean(prop.getProperty("is_secure"));
		
		AmazonS3 svc = getConn(endpoint, accessKey, secretKey, issecure);

		return svc;
	}
	
	public AmazonS3 getAWS4CLI() {
		
		try {
			input = new FileInputStream("config.properties");
			try {
				prop.load(input);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String endpoint = prop.getProperty("endpoint");
		String accessKey = prop.getProperty("access_key");
		String secretKey = prop.getProperty("access_secret");
		Boolean issecure = Boolean.parseBoolean(prop.getProperty("is_secure"));
		
		AmazonS3 svc = getAWS4Conn(endpoint, accessKey, secretKey, issecure);

		return svc;
	}
	
	public String getPrefix()
	{
		String prefix = "jnan";
		
		return prefix;
	}
	
	
	@SuppressWarnings("deprecation")
	public AmazonS3 getConn(String endpoint,String accessKey, String secretKey,Boolean issecure) {
		
		ClientConfiguration clientConfig = new ClientConfiguration();
		AmazonS3ClientBuilder.standard().setEndpointConfiguration(
				new AwsClientBuilder.EndpointConfiguration(endpoint, "mexico"));
		
		if (issecure){
			clientConfig.setProtocol(Protocol.HTTP);
		}else {
			
			clientConfig.setProtocol(Protocol.HTTP);
		}
		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		@SuppressWarnings("deprecation")
		AmazonS3 s3client = new AmazonS3Client(credentials); //

		s3client.setEndpoint(endpoint);

		s3client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
		
		return s3client;
	}
	
	@SuppressWarnings("deprecation")
	public AmazonS3 getAWS4Conn(String endpoint,String accessKey, String secretKey,Boolean issecure) {
		
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setSignerOverride("AWSS3V4SignerType");
		AmazonS3ClientBuilder.standard().setEndpointConfiguration(
				new AwsClientBuilder.EndpointConfiguration(endpoint, "mexico"));
		
		if (issecure){
			clientConfig.setProtocol(Protocol.HTTP);
		}else {
			
			clientConfig.setProtocol(Protocol.HTTPS);
		}
		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		@SuppressWarnings("deprecation")
		AmazonS3 s3client = new AmazonS3Client(credentials, clientConfig); 

		s3client.setEndpoint(endpoint);

		s3client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
		
		return s3client;
	}
	
	public String getBucketName(String prefix) {
		
		Random rand = new Random(); 
		int num = rand.nextInt(50);
		String randomStr = UUID.randomUUID().toString();
		
		return prefix + randomStr + num;
	}
	
	public String getBucketName() {
		String prefix = prop.getProperty("bucket_prefix");
		
		Random rand = new Random(); 
		int num = rand.nextInt(50);
		String randomStr = UUID.randomUUID().toString();
		
		return prefix + randomStr + num;
	}
	public String repeat(String str, int count){
	    if(count <= 0) {return "";}
	    return new String(new char[count]).replace("\0", str);
	}
	
	public void tearDown() {
		
		java.util.List<Bucket> buckets = svc.listBuckets();
		String prefix = prop.getProperty("bucket_prefix");
		
		for (Bucket b : buckets) {
			
			String bucket_name = b.getName();
			
			if (b.getName().startsWith(prefix)) {
				
				ObjectListing object_listing = svc.listObjects(b.getName());
				while (true) {
	                for (java.util.Iterator<S3ObjectSummary> iterator =
	                        object_listing.getObjectSummaries().iterator();
	                        iterator.hasNext();) {
	                    S3ObjectSummary summary = (S3ObjectSummary)iterator.next();
	                    svc.deleteObject(bucket_name, summary.getKey());
	                }

	                if (object_listing.isTruncated()) {
	                    object_listing = svc.listNextBatchOfObjects(object_listing);
	                } else {
	                    break;
	                }
	            };
	            
	            VersionListing version_listing = svc.listVersions(
	                    new ListVersionsRequest().withBucketName(bucket_name));
	            while (true) {
	                for (java.util.Iterator<S3VersionSummary> iterator =
	                        version_listing.getVersionSummaries().iterator();
	                        iterator.hasNext();) {
	                    S3VersionSummary vs = (S3VersionSummary)iterator.next();
	                    svc.deleteVersion(
	          
	                  bucket_name, vs.getKey(), vs.getVersionId());
	                }

	                if (version_listing.isTruncated()) {
	                    version_listing = svc.listNextBatchOfVersions(
	                            version_listing);
	                } else {
	                    break;
	                }
	            }
			    svc.deleteBucket(new DeleteBucketRequest(b.getName()));
			}
		}
	}
	
	private static SecretKey generateSecretKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }
	
	public String[] EncryptionSseCustomerWrite(int file_size) {
		
		String prefix = prop.getProperty("bucket_prefix");
		String bucket_name = getBucketName(prefix);
		String key ="key1";
		String data = repeat("testcontent", file_size);
		
		svc.createBucket(bucket_name);	
		
		SecretKey secretKey = generateSecretKey();
        SSECustomerKey sseKey = new SSECustomerKey(secretKey);
		
		PutObjectRequest putRequest = new PutObjectRequest(bucket_name, key, data).withSSECustomerKey(sseKey);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(data.length());
		objectMetadata.setHeader("x-amz-server-side-encryption-customer-key", "pO3upElrwuEXSoFwCfnZPdSsmt/xWeFa0N9KgDijwVs=");
		objectMetadata.setHeader("x-amz-server-side-encryption-customer-key-md5", "DWygnHRtgiJ77HCm+1rvHw==");
		putRequest.setMetadata(objectMetadata);
		svc.putObject(putRequest);
		
		
		String rdata = svc.getObjectAsString(bucket_name, key);
		
		//trying to return two values
		String arr[] = new String[2];
        arr[0]= data;
        arr[1] =  rdata;
		
		return arr;
				
	}
	
	public String[] EncryptionSseKMSCustomerWrite(int file_size, String keyId) {
		
		String prefix = prop.getProperty("bucket_prefix");
		String bucket_name = getBucketName(prefix);
		String key ="key1";
		String data = repeat("testcontent", file_size);
		
		if (keyId == "") {
			keyId = "testkey-1";
		}
		
		
		svc.createBucket(bucket_name);	
		
		PutObjectRequest putRequest = new PutObjectRequest(bucket_name, key, data);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(data.length());
		objectMetadata.setHeader("x-amz-server-side-encryption", "aws:kms");
		objectMetadata.setHeader("x-amz-server-side-encryption-aws-kms-key-id", keyId );
		putRequest.setMetadata(objectMetadata);
		svc.putObject(putRequest);
		
		
		String rdata = svc.getObjectAsString(bucket_name, key);
		
		//trying to return two values
		String arr[] = new String[2];
        arr[0]= data;
        arr[1] =  rdata;
		
		return arr;
				
	}

	public Bucket createKeys(String[] keys) {
		
		String prefix = prop.getProperty("bucket_prefix");
		String bucket_name = getBucketName(prefix);
		Bucket bucket = svc.createBucket(bucket_name);
		
		for (String k : keys) {
			
			svc.putObject(bucket.getName(), k, k);
			
		}
		
		return bucket;		
	}
	
	public ObjectMetadata getSetMetadata(String metadata) {
		
		String prefix = prop.getProperty("bucket_prefix");
		String bucket_name = getBucketName(prefix);
		String content = "testcontent";
		String key = "key1";
		
		byte[] contentBytes = content.getBytes(StringUtils.UTF8);
		InputStream is = new ByteArrayInputStream(contentBytes);
		
		ObjectMetadata mdata = new ObjectMetadata();
		mdata.setContentLength(contentBytes.length);
		mdata.addUserMetadata("Mymeta", metadata);
		
		svc.putObject(new PutObjectRequest(bucket_name, key, is, mdata));
		
		S3Object resp = svc.getObject(new GetObjectRequest(bucket_name, key));
		
		return resp.getObjectMetadata();
	}
	
	public void checkSSL() {
		Boolean issecure = Boolean.parseBoolean(prop.getProperty("is_secure"));
		if(issecure == false){
			throw new SkipException("This operation needs HTTPs connection ");
		}else{
		System.out.println("I am in else condition");	
		}
	}
	
	public void checkKeyId() {
		String kmskeyid = prop.getProperty("kmskeyid");
		if(kmskeyid == null){
			throw new SkipException("No keyId provided for this Operation");
		}else{
		System.out.println("I am in else condition");	
		}
	}
	
	public <PartETag> CompleteMultipartUploadRequest multipartUploadLLAPI(AmazonS3 svc, String bucket, String key, long size, String filePath) {
		
		List<PartETag> partETags = new ArrayList<PartETag>();

		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, key);
		InitiateMultipartUploadResult initResponse = svc.initiateMultipartUpload(initRequest);

		File file = new File(filePath);
		long contentLength = file.length();
		long partSize = size;

		
			
		    long filePosition = 0;
		    for (int i = 1; filePosition < contentLength; i++) {
		    	
		    	partSize = Math.min(partSize, (contentLength - filePosition));
		    	
		        // Create request to upload a part.
		        UploadPartRequest uploadRequest = new UploadPartRequest()
		            .withBucketName(bucket).withKey(key)
		            .withUploadId(initResponse.getUploadId()).withPartNumber(i)
		            .withFileOffset(filePosition)
		            .withFile(file)
		            .withPartSize(partSize);
		        
		        partETags.add((PartETag) svc.uploadPart(uploadRequest).getPartETag());

		        filePosition += partSize;
		    }

		    CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, 
		                                               initResponse.getUploadId(), 
		                                               (List<com.amazonaws.services.s3.model.PartETag>) partETags);
		    
		    return compRequest;
		    
	}
	
	public CompleteMultipartUploadRequest multipartCopy(AmazonS3 svc,String dstbkt, String dstkey, String srcbkt, String srckey,long size) {
		
		List<CopyPartResult> copyResponses = new ArrayList<CopyPartResult>();

		InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(dstbkt, dstkey);
		        
		InitiateMultipartUploadResult initResult = svc.initiateMultipartUpload(initiateRequest);

		String uploadId = initResult.getUploadId();

		
			
		    GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(srcbkt, srckey);

		    ObjectMetadata metadataResult = svc.getObjectMetadata(metadataRequest);
		    long objectSize = metadataResult.getContentLength(); 
		    
		    long partSize = size; 
		    long bytePosition = 0;
		    for (int i = 1; bytePosition < objectSize; i++)
		    {
		    	CopyPartRequest copyRequest = new CopyPartRequest()
		           .withDestinationBucketName(dstbkt)
		           .withDestinationKey(dstkey)
		           .withSourceBucketName(srcbkt)
		           .withSourceKey(srckey)
		           .withUploadId(initResult.getUploadId())
		           .withFirstByte(bytePosition)
		           .withLastByte(bytePosition + partSize -1 >= objectSize ? objectSize - 1 : bytePosition + partSize - 1) 
		           .withPartNumber(i);

		        copyResponses.add(svc.copyPart(copyRequest));
		        bytePosition += partSize;
		    }
		    CompleteMultipartUploadRequest copyRequest = new CompleteMultipartUploadRequest().withBucketName(dstbkt).withKey(dstkey).withUploadId(initResult.getUploadId());
		    
		    return copyRequest;
	}
	
	public S3Object[] getbktContent(String src_bkt, String src_key, String dst_bkt, String dst_key) {
			
			GetObjectRequest rangeObjectsrc = new GetObjectRequest(src_bkt, src_key);
	        rangeObjectsrc.setRange(0, 10);
	        S3Object objectPortion = svc.getObject(rangeObjectsrc);
	        
	        GetObjectRequest rangeObjectdst = new GetObjectRequest(src_bkt, src_key);
	        rangeObjectdst.setRange(0, 10);
	        S3Object objectPortiondst = svc.getObject(rangeObjectdst);
	        
	        S3Object arr[] = new S3Object[2];
	             arr[0]= objectPortion;
	             arr[1] =  objectPortiondst;
	      		
	      		return arr;
	      				
			
	}
	
	public Upload multipartUploadFileHLAPI(AmazonS3 svc,String bucket, String key, String filePath) { 
        
        @SuppressWarnings("deprecation")
		TransferManager tm = new TransferManager(svc);  
        Upload upload = tm.upload(bucket, key, new File(filePath));
        try {
			try {
				upload.waitForCompletion();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (AmazonClientException amazonClientException) {
			
			amazonClientException.printStackTrace();
		}
		
        return upload;
	}
	
	public Transfer multipartUploadDirectoryHLAPI(AmazonS3 svc, String bucket, String s3target, String directory) throws AmazonServiceException, AmazonClientException, InterruptedException { 
        
		@SuppressWarnings("deprecation")
		TransferManager tm = new TransferManager(svc);
		Transfer t = tm.uploadDirectory(bucket, s3target, new File(directory), false);
	    try {
	      t.waitForCompletion();
	    } finally {
	      tm.shutdownNow(false);
	    }
	    
	    return t;
		
	}
}