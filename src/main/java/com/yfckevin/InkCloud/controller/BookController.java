package com.yfckevin.InkCloud.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import com.yfckevin.InkCloud.ConfigProperties;
import com.yfckevin.InkCloud.dto.BookDTO;
import com.yfckevin.InkCloud.dto.ImageRequestDTO;
import com.yfckevin.InkCloud.dto.SearchDTO;
import com.yfckevin.InkCloud.entity.Book;
import com.yfckevin.InkCloud.entity.ErrorFile;
import com.yfckevin.InkCloud.exception.ResultStatus;
import com.yfckevin.InkCloud.service.BookService;
import com.yfckevin.InkCloud.service.ErrorFileService;
import com.yfckevin.InkCloud.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
public class BookController {
    Logger logger = LoggerFactory.getLogger(BookController.class);
    private final ConfigProperties configProperties;
    private final SimpleDateFormat picSuffix;
    private final ObjectMapper objectMapper;
    private final BookService bookService;
    private final OpenAiService openAiService;
    private final ErrorFileService errorFileService;
    private final SimpleDateFormat sdf;

    public BookController(ConfigProperties configProperties, @Qualifier("picSuffix") SimpleDateFormat picSuffix, ObjectMapper objectMapper, BookService bookService, OpenAiService openAiService, ErrorFileService errorFileService, @Qualifier("sdf") SimpleDateFormat sdf) {
        this.configProperties = configProperties;
        this.picSuffix = picSuffix;
        this.objectMapper = objectMapper;
        this.bookService = bookService;
        this.openAiService = openAiService;
        this.errorFileService = errorFileService;
        this.sdf = sdf;
    }


    /**
     * 批量存書
     *
     * @param imageRequestDTOs
     * @return
     */
    @PostMapping("/saveMultiBook")
    public ResponseEntity<?> saveMultiBook(@RequestBody List<ImageRequestDTO> imageRequestDTOs) {
        ResultStatus resultStatus = new ResultStatus();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 成功筆數和錯誤筆數
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (ImageRequestDTO imageDTO : imageRequestDTOs) {
            final String originalFileName = imageDTO.getFileName(); // 使用傳送過來的檔案名稱

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (String base64Image : imageDTO.getImages()) {
                    try {
                        final byte[] decodeImg = Base64.getDecoder().decode(base64Image);

                        // 生成唯一檔案名稱，避免覆蓋
                        String fileName = generateUniqueFileName(originalFileName);
                        String filePath = configProperties.getAiPicSavePath() + fileName;

                        // 儲存圖片
                        try (FileOutputStream fos = new FileOutputStream(filePath)) {
                            fos.write(decodeImg);
                        }

                        ByteString imgBytes = ByteString.copyFrom(decodeImg);
                        Image img = Image.newBuilder().setContent(imgBytes).build();
                        Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
                        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                                .addFeatures(feature)
                                .setImage(img)
                                .build();

                        List<AnnotateImageRequest> requests = new ArrayList<>();
                        requests.add(request);

                        String rawText = bookService.extractText(requests);
                        rawText = rawText.replaceAll("\\r?\\n", "");
                        logger.info("圖轉文：{}", rawText);

                        final ResultStatus<String> completionResult = openAiService.completion(rawText);
                        final String code = completionResult.getCode();
                        final String message = completionResult.getMessage();
                        ErrorFile errorFile = new ErrorFile();

                        if ("C000".equals(code)) {
                            final String content = completionResult.getData();
                            List<Book> bookList = objectMapper.readValue(content, new TypeReference<>() {
                            });
                            bookList = bookList.stream()
                                    .peek(book -> {
                                        book.setCreationDate(sdf.format(new Date()));
                                        book.setSourceCoverName(fileName);
                                    })
                                    .collect(Collectors.toList());
                            bookService.saveAll(bookList);
                            logger.info("成功處理並保存書籍，檔案名: {}", filePath);
                            successCount.incrementAndGet(); // 增加成功筆數
                        } else {
                            errorFile.setErrorCode(code);
                            errorFile.setErrorMsg(message);
                            errorFile.setCoverName(fileName);
                            ErrorFile savedErrorFile = errorFileService.save(errorFile);
                            logger.error("處理異常，檔案名: {}", filePath);
                            errorCount.incrementAndGet(); // 增加錯誤筆數
                        }
                    } catch (Exception e) {
                        ErrorFile errorFile = new ErrorFile();
                        errorFile.setErrorMsg(e.getMessage());
                        errorFile.setCoverName(originalFileName);
                        ErrorFile savedErrorFile = errorFileService.save(errorFile);
                        logger.error("處理檔案 {} 時發生異常: {}", originalFileName, e.getMessage(), e);
                        errorCount.incrementAndGet(); // 增加錯誤筆數
                    }
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有異步任務完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        allOf.join();
        executorService.shutdown();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(Map.of(
                "successCount", successCount.get(),
                "errorCount", errorCount.get()
        ));

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 刪書
     * @param id
     * @return
     */
    @DeleteMapping("/deleteBook/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        ResultStatus resultStatus = new ResultStatus();
        bookService.findById(id)
                .ifPresent(
                        book -> {
                           book.setDeletionDate(sdf.format(new Date()));
                           bookService.save(book);
                        });
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 修書
     * @return
     */
    @PostMapping("/editBook")
    public ResponseEntity<?> editBook (@RequestBody BookDTO bookDTO){
        bookService.findById(bookDTO.getId()).ifPresent(book -> {
            book.setTitle(bookDTO.getTitle());
            book.setAuthor(bookDTO.getAuthor());
            book.setPublisher(bookDTO.getPublisher());
            bookService.save(book);
        });
        ResultStatus resultStatus = new ResultStatus();
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 查書
     * @param searchDTO
     * @return
     */
    @PostMapping("/searchBook")
    public ResponseEntity<?> searchBook (@RequestBody SearchDTO searchDTO){
        List<Book> bookList = bookService.findBook(searchDTO);
        final List<BookDTO> bookDTOList = bookList.stream()
                .map(BookController::constructBookDTO)
                .sorted(Comparator.comparing(BookDTO::getCreationDate).reversed())
                .toList();
        ResultStatus resultStatus = new ResultStatus();
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(bookDTOList);
        return ResponseEntity.ok(resultStatus);
    }

    private static BookDTO constructBookDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setTitle(book.getTitle());
        dto.setId(book.getId());
        dto.setPublisher(book.getPublisher());
        dto.setAuthor(book.getAuthor());
        dto.setCreationDate(book.getCreationDate());
        return dto;
    }


    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }

        String baseName = originalFileName.substring(0, dotIndex);

        return baseName + "_" + System.currentTimeMillis() + extension;
    }

}
