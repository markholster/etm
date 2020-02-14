/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

onmessage = function (event) {
    importScripts('highlight.pack.min.js');
    var result;
    if ('HTML' == event.data[1] || 'SOAP' == event.data[1] || 'XML' == event.data[1]) {
        result = hljs.highlight('xml', event.data[0], true);
    } else if ('SQL' == event.data[1]) {
        result = hljs.highlight('sql', event.data[0], true);
    } else if ('JSON' == event.data[1]) {
        result = hljs.highlight('json', event.data[0], true);
    } else {
        result = hljs.highlightAuto(event.data[0]);
    }
    postMessage(result.value);
}