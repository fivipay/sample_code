<?php
include('Crypt/RSA.php');

	function Decrypt($input, $key_seed)
	{
		$input = base64_decode($input);
		$key = substr(md5($key_seed),0,24);
		$text=mcrypt_decrypt(MCRYPT_TRIPLEDES, $key, $input,
		MCRYPT_MODE_ECB,'12345678');
		$block = mcrypt_get_block_size('tripledes', 'ecb');
		$packing = ord($text{strlen($text) - 1});
		if($packing and ($packing < $block)){
			for($P = strlen($text) - 1; $P >= strlen($text) - $packing; $P--){
				if(ord($text{$P}) != $packing){
					$packing = 0;
				}
			}
		}
		$text = substr($text,0,strlen($text) - $packing);
		return $text;
	}

	function execPostRequest($url, $data)
	{
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_URL, $url);
		curl_setopt($ch, CURLOPT_POST, 1);
		curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
		curl_setopt($ch, CURLOPT_HTTPHEADER, array(                                                                          
		    'Content-Type: application/json',                                                                                
		    'Content-Length: ' . strlen($data))                                                                       
		);  
		curl_setopt($ch, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT']);
		curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
		curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 2);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
		$result = curl_exec($ch);
		curl_close($ch);
		return $result;
	}
$username = ''; // thay bang thong tin cua doi tac
$api_code = ''; // thay bang thong tin cua doi tac
$api_username = ''; // thay bang thong tin cua doi tac
$api_password = ''; // thay bang thong tin cua doi tac
$request_id = uniqid();
$service_code = ''; // xem trong tai lieu
$price = 10000; // gia the
$quantity= 1; // so luong the 

$string_data = $username."|".$api_code."|".$api_username."|".$service_code."|".$request_id;
$private_key = file_get_contents('private.der');
$rsa = new Crypt_RSA();
$rsa->loadKey($private_key); // public key
$plaintext = $string_data;
$privatekey = $rsa->getPrivateKey();	
openssl_sign($plaintext, $signature, $privatekey);
$data_sign =base64_encode($signature);

$data_array = array(
	"username" => $username,
	"apiCode" => $api_code,
	"apiUsername" => $api_username,
	"requestId" => $request_id,
	"serviceCode" => $service_code,
	"price" => $price,
	"quantity" => $quantity,
	"dataSign" => $data_sign,
	
	);
$data_json = json_encode($data_array);                                                                                   
//send request lay the 
$send_request = execPostRequest('https://merchant.fivipay.com/api/v1/service/requestTransaction', $data_json);
$send_request = json_decode($send_request, true);
$card_info_encrypt = $send_request['encryptCards'];

//in thong tin the
echo (Decrypt($card_info_encrypt, $api_password));
?>