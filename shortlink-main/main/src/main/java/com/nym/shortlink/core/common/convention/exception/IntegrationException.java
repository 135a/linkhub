/*
 * Copyright © 2026 NageOffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nym.shortlink.core.common.convention.exception;

import com.nym.shortlink.core.common.convention.errorcode.BaseErrorCode;
import com.nym.shortlink.core.common.convention.errorcode.IErrorCode;

/**
 * 远程服务调用异常
 */
public class IntegrationException extends AbstractException {

    public IntegrationException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public IntegrationException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public IntegrationException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "IntegrationException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
