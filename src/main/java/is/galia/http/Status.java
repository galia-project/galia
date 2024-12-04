/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.http;

/**
 * Response status.
 *
 * @see <a href="https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml">
 *     HTTP Status Code Registry</a>
 */
public record Status(int code, String description) {

    public static final Status CONTINUE                        = new Status(100);
    public static final Status SWITCHING_PROTOCOLS             = new Status(101);
    public static final Status PROCESSING                      = new Status(102);
    public static final Status EARLY_HINTS                     = new Status(103);
    public static final Status OK                              = new Status(200);
    public static final Status CREATED                         = new Status(201);
    public static final Status ACCEPTED                        = new Status(202);
    public static final Status NON_AUTHORITATIVE_INFORMATION   = new Status(203);
    public static final Status NO_CONTENT                      = new Status(204);
    public static final Status RESET_CONTENT                   = new Status(205);
    public static final Status PARTIAL_CONTENT                 = new Status(206);
    public static final Status MULTI_STATUS                    = new Status(207);
    public static final Status ALREADY_REPORTED                = new Status(208);
    public static final Status IM_USED                         = new Status(226);
    public static final Status MULTIPLE_CHOICES                = new Status(300);
    public static final Status MOVED_PERMANENTLY               = new Status(301);
    public static final Status FOUND                           = new Status(302);
    public static final Status SEE_OTHER                       = new Status(303);
    public static final Status NOT_MODIFIED                    = new Status(304);
    public static final Status USE_PROXY                       = new Status(305);
    public static final Status UNUSED                          = new Status(306);
    public static final Status TEMPORARY_REDIRECT              = new Status(307);
    public static final Status PERMANENT_REDIRECT              = new Status(308);
    public static final Status BAD_REQUEST                     = new Status(400);
    public static final Status UNAUTHORIZED                    = new Status(401);
    public static final Status PAYMENT_REQUIRED                = new Status(402);
    public static final Status FORBIDDEN                       = new Status(403);
    public static final Status NOT_FOUND                       = new Status(404);
    public static final Status METHOD_NOT_ALLOWED              = new Status(405);
    public static final Status NOT_ACCEPTABLE                  = new Status(406);
    public static final Status PROXY_AUTHENTICATION_REQUIRED   = new Status(407);
    public static final Status REQUEST_TIMEOUT                 = new Status(408);
    public static final Status CONFLICT                        = new Status(409);
    public static final Status GONE                            = new Status(410);
    public static final Status LENGTH_REQUIRED                 = new Status(411);
    public static final Status PRECONDITION_FAILED             = new Status(412);
    public static final Status PAYLOAD_TOO_LARGE               = new Status(413);
    public static final Status URI_TOO_LONG                    = new Status(414);
    public static final Status UNSUPPORTED_MEDIA_TYPE          = new Status(415);
    public static final Status RANGE_NOT_SATISFIABLE           = new Status(416);
    public static final Status EXPECTATION_FAILED              = new Status(417);
    public static final Status MISDIRECTED_REQUEST             = new Status(421);
    public static final Status UNPROCESSABLE_ENTITY            = new Status(422);
    public static final Status LOCKED                          = new Status(423);
    public static final Status FAILED_DEPENDENCY               = new Status(424);
    public static final Status TOO_EARLY                       = new Status(425);
    public static final Status UPGRADE_REQUIRED                = new Status(426);
    public static final Status PRECONDITION_REQUIRED           = new Status(428);
    public static final Status TOO_MANY_REQUESTS               = new Status(429);
    public static final Status REQUEST_HEADER_FIELDS_TOO_LARGE = new Status(431);
    public static final Status UNAVAILABLE_FOR_LEGAL_REASONS   = new Status(451);
    public static final Status INTERNAL_SERVER_ERROR           = new Status(500);
    public static final Status NOT_IMPLEMENTED                 = new Status(501);
    public static final Status BAD_GATEWAY                     = new Status(502);
    public static final Status SERVICE_UNAVAILABLE             = new Status(503);
    public static final Status GATEWAY_TIMEOUT                 = new Status(504);
    public static final Status HTTP_VERSION_NOT_SUPPORTED      = new Status(505);
    public static final Status VARIANT_ALSO_NEGOTIATES         = new Status(506);
    public static final Status INSUFFICIENT_STORAGE            = new Status(507);
    public static final Status LOOP_DETECTED                   = new Status(508);
    public static final Status NOT_EXTENDED                    = new Status(510);
    public static final Status NETWORK_AUTHENTICATION_REQUIRED = new Status(511);

    public Status(int code) {
        this(code, switch (code) {
            case 100 -> "Continue";
            case 101 -> "Switching Protocols";
            case 102 -> "Processing";
            case 103 -> "Early Hints";
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 203 -> "Non-Authoritative Information";
            case 204 -> "No Content";
            case 205 -> "Reset Content";
            case 206 -> "Partial Content";
            case 207 -> "Multi-Status";
            case 208 -> "Already Reported";
            case 226 -> "IM Used";
            case 300 -> "Multiple Choices";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 303 -> "See Other";
            case 304 -> "Not Modified";
            case 305 -> "Use Proxy";
            case 307 -> "Temporary Redirect";
            case 308 -> "Permanent Redirect";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 402 -> "Payment Required";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 407 -> "Proxy Authentication Required";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 411 -> "Length Required";
            case 412 -> "Precondition Failed";
            case 413 -> "Payload Too Large";
            case 414 -> "URI Too Long";
            case 415 -> "Unsupported Media Type";
            case 416 -> "Range Not Satisfiable";
            case 417 -> "Expectation Failed";
            case 421 -> "Misdirected Request";
            case 422 -> "Unprocessable Entity";
            case 423 -> "Locked";
            case 424 -> "Failed Dependency";
            case 425 -> "Too Early";
            case 426 -> "Upgrade Required";
            case 428 -> "Precondition Required";
            case 429 -> "Too Many Requests";
            case 431 -> "Request Header Fields Too Large";
            case 451 -> "Unavailable For Legal Reasons";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            case 505 -> "HTTP Version Not Supported";
            case 506 -> "Variant Also Negotiates";
            case 507 -> "Insufficient Storage";
            case 508 -> "Loop Detected";
            case 510 -> "Not Extended";
            case 511 -> "Network Authentication Required";
            default  -> "Unknown";
        });
    }

    public boolean isClientError() {
        return code >= 400 && code < 500;
    }

    public boolean isError() {
        return code >= 400;
    }

    public boolean isServerError() {
        return code >= 500;
    }

    @Override
    public String toString() {
        return code + " " + description();
    }

}
