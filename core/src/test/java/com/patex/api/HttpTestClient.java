package com.patex.api;


import org.apache.http.client.methods.HttpGet;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Alexey on 8/15/2016.
 */
public class HttpTestClient {

    private final String url;


    public HttpTestClient(String url) {
        this.url = url;

    }


    public <M, E extends List<M>> ResponseEntity<E> uploadFiles(String path, String filesPropName, Map<String, InputStream> files, ParameterizedTypeReference<E> responseType) {
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.put(filesPropName,
                files.entrySet().stream().
                        map(entry -> new ClassPathResource(entry.getKey())).
                        collect(Collectors.toList()));
//                        map(entry -> new InputStreamResource(entry.getValue(), entry.getKey())).
        return post(path, map, MediaType.MULTIPART_FORM_DATA, responseType);
    }

    public <E,M> ResponseEntity<E> post(String path,
                                        M body,
                                        MediaType mediaType,
                                        ParameterizedTypeReference<E> responseType) {
        RestTemplate template = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);

        HttpEntity<M> requestEntity = new HttpEntity<M>(body, headers);
        return template.exchange(url + "/" + path,
                HttpMethod.POST, requestEntity, responseType);
    }

    public <E,M> ResponseEntity<E> post(String path,
                                        M body,
                                        MediaType mediaType,
                                        Class<E> responseType) {
        RestTemplate template = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);

        HttpEntity<M> requestEntity = new HttpEntity<M>(body, headers);
        return template.exchange(url + "/" + path, HttpMethod.POST, requestEntity, responseType);
    }



    public <T> T get(String uri,Class<T> clazz) throws IOException {
        RestTemplate template = new RestTemplate();
        return template.getForObject(url+"/"+uri, clazz);
    }


//    public <E> E getReguest(){
//        RestTemplate restTemplate = new RestTemplate();
//
//    }

}
