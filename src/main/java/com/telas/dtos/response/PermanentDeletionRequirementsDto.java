package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermanentDeletionRequirementsDto {
    private boolean requiresMonitorSuccessor;
    private int monitorCount;
}
