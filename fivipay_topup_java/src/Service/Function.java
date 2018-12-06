package Service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;

import org.json.JSONObject;

public class Function {

	public static String private_key = "key/private.der"; // thay bang file
															// private.der cua
															// mc
	public static String username = ""; // ten tk login vao mytopup.
	public static String apiCode = ""; // ma api tk, duoc cung cap khi dang ky
										// tk
	public static String apiUsername = ""; // ten api tk, duoc cung cap khi dang
											// ky tk
	public static String apiPassword = ""; // dung de giai ma khi mua ma the

	public static void main(String[] args) throws Exception {
		Function function = new Function();
		function.checkBalance(); // kiem tra so du
		// function.topupTelco(); //nap tien dien thoai
		// function.buyCard(); //mua ma the
		// function.queryApi(); //kiem tra lai giao dich
	}

	// Topup
	public void topupTelco() throws Exception {
		String serviceCode = ""; // xem trong tai lieu muc 3.1
		String requestId = String.valueOf(System.currentTimeMillis());
		int price = 10000; // menh gia nap
		String phoneNumber = ""; // so dien thoai muon nap tien

		String plainSign = username + "|" + apiCode + "|" + apiUsername + "|" + serviceCode + "|" + requestId;
		String dataSign = createSign(plainSign, private_key);
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("apiCode", apiCode);
		json.put("apiUsername", apiUsername);
		json.put("requestId", requestId);
		json.put("serviceCode", serviceCode);
		json.put("price", price);
		json.put("phoneNumber", phoneNumber);
		json.put("dataSign", dataSign);
		String response = sendPost(json.toString());
		System.out.println("Topup:" + response);
	}

	// BuyCard
	public void buyCard() throws Exception {
		String serviceCode = ""; // Xem trong tai lieu muc 3.1
		String requestId = String.valueOf(System.currentTimeMillis());
		int price = 10000; // menh gia the
		int quantity = 1; // max 50.

		String plainSign = username + "|" + apiCode + "|" + apiUsername + "|" + serviceCode + "|" + requestId;
		String dataSign = createSign(plainSign, private_key);
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("apiCode", apiCode);
		json.put("apiUsername", apiUsername);
		json.put("requestId", requestId);
		json.put("serviceCode", serviceCode);
		json.put("price", price);
		json.put("quantity", quantity);
		json.put("dataSign", dataSign);
		String response = sendPost(json.toString());
		System.out.println("Response:" + response);

		JSONObject jObj = new JSONObject(response);
		String encryptCards = jObj.getString("encryptCards");
		System.out.println(encryptCards);
		String listCard = decrypt(apiPassword, encryptCards);
		System.out.println("ListCard:" + listCard);
	}

	// Check balance
	public void checkBalance() throws Exception {
		String serviceCode = ""; // Xem trong tai lieu muc 3.1
		String requestId = String.valueOf(System.currentTimeMillis());

		String plainSign = username + "|" + apiCode + "|" + apiUsername + "|" + serviceCode + "|" + requestId;
		String dataSign = createSign(plainSign, private_key);
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("apiCode", apiCode);
		json.put("apiUsername", apiUsername);
		json.put("requestId", requestId);
		json.put("serviceCode", serviceCode);
		json.put("dataSign", dataSign);
		String response = sendPost(json.toString());
		System.out.println(response);

		JSONObject jObj = new JSONObject(response);
		Double balance = jObj.getDouble("balance");
		System.out.println("Balance:" + balance);

	}

	// Query Api
	public void queryApi() throws Exception {
		String target = "";
		String type = "";
		String serial = "";
		Double price = null;
		String serviceCode = ""; // xem trong tai lieu muc 3.1
		String requestId = String.valueOf(System.currentTimeMillis());
		String transactionId = ""; // ma giao dich
		String txnRequestId = ""; // truong hop time out ko nhan dc
									// transactionId co the dung requestID mc da
									// tao ra truoc do de query lai giao dich.
									// tham so gui query api se la txnRequestId

		String plainSign = username + "|" + apiCode + "|" + apiUsername + "|" + serviceCode + "|" + requestId;
		String dataSign = createSign(plainSign, private_key);
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("apiCode", apiCode);
		json.put("apiUsername", apiUsername);
		json.put("transactionId", transactionId);
		json.put("serviceCode", serviceCode);
		json.put("transactionId", transactionId);	// truong hop time out ko nhan dc
													// transactionId co the dung requestID mc da
													// tao ra truoc do de query lai giao dich.
													// tham so gui query api se la txnRequestId
		json.put("dataSign", dataSign);
		String response = sendPost(json.toString());
	//	System.out.println("Response:" + response);
		
		JSONObject jObj = new JSONObject(response);
		JSONObject transaction = jObj.getJSONObject("transaction");
		JSONArray array = transaction.getJSONArray("serials");
	//	System.out.println(array);
		for (int i = 0; i < array.length(); i++) {
			JSONObject ob = array.getJSONObject(i);
			target = ob.getString("pin");
			serial = ob.getString("serial");
			price = ob.getDouble("price");
			type = ob.getString("cardType");
		}
		String pin = decrypt(apiPassword, target);
		System.out.println("Card:"+ type+ "|" + price + "|" + pin + "|" +serial);

	}

	// create dataSign
	public static String createSign(String data, String filePath) {
		try {
			final File privKeyFile = new File(filePath);
			final byte[] privKeyBytes = readFile(privKeyFile);
			final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			final PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
			final PrivateKey pk = (PrivateKey) keyFactory.generatePrivate(privSpec);

			final Signature sg = Signature.getInstance("SHA1withRSA");

			sg.initSign(pk);
			sg.update(data.getBytes());
			final byte[] bDS = sg.sign();
			return new String(org.apache.commons.codec.binary.Base64.encodeBase64(bDS));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}

	// read file
	public static byte[] readFile(final File file) throws FileNotFoundException, IOException {
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(file));
			final byte[] data = new byte[(int) file.length()];
			dis.readFully(data);
			return data;
		} finally {
			if (dis != null) {
				dis.close();
			}
		}
	}

	// decrypt card
	public static String decrypt(String key, String data) throws Exception {
		Cipher cipher = Cipher.getInstance("TripleDES");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(key.getBytes(), 0, key.length());
		String keymd5 = new BigInteger(1, md5.digest()).toString(16).substring(0, 24);
		SecretKeySpec keyspec = new SecretKeySpec(keymd5.getBytes(), "TripleDES");
		cipher.init(Cipher.DECRYPT_MODE, keyspec);
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] raw = decoder.decodeBuffer(data);
		byte[] stringBytes = cipher.doFinal(raw);
		String result = new String(stringBytes);
		return result;
	}

	// sendPost
	public String sendPost(String param) throws Exception {
		String url = "https://merchant.fivipay.com/api/v1/service/requestTransaction";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(param);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("URL : " + url);
		System.out.println("Post parameters : " + param);
		System.out.println("Response Code : " + responseCode);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		String json = response.toString();
		return json;
	}

}
