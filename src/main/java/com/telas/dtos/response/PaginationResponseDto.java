package com.telas.dtos.response;

import java.io.Serial;
import java.io.Serializable;

public class PaginationResponseDto<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1209210235191495372L;
    private Integer totalRecords;
    private Integer totalPages;
    private Integer currentPage;
    private transient T list;

    private PaginationResponseDto() {
    }

    public static <T> PaginationResponseDto<T> fromResult(T list, Integer totalRecords, Integer totalPages, Integer currentPage) {
        return new PaginationResponseDto<T>()
                .setTotalRecords(totalRecords)
                .setTotalPages(totalPages)
                .setCurrentPage(currentPage)
                .setList(list);
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public PaginationResponseDto<T> setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
        return this;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public PaginationResponseDto<T> setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
        return this;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public PaginationResponseDto<T> setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
        return this;
    }

    public T getList() {
        return list;
    }

    public PaginationResponseDto<T> setList(T resultado) {
        list = resultado;
        return this;
    }
}
