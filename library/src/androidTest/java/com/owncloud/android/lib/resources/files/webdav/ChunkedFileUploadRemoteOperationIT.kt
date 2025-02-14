/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.lib.resources.files.webdav

import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.common.network.WebdavEntry
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation
import junit.framework.TestCase
import junit.framework.TestCase.assertNotNull
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod
import org.junit.Test
import java.io.File

class ChunkedFileUploadRemoteOperationIT : AbstractIT() {
    @Test
    fun uploadWifi() {
        val sut = genLargeUpload(true)
        val uploadResult = sut.execute(client)
        assert(uploadResult.isSuccess)
    }

    @Test
    fun uploadMobile() {
        val sut = genLargeUpload(false)
        val uploadResult = sut.execute(client)
        assert(uploadResult.isSuccess)
    }

    @Test
    fun cancel() {
        val sut = genLargeUpload(false)

        var uploadResult: RemoteOperationResult<String>? = null
        Thread {
            uploadResult = sut.execute(client)
        }.start()

        shortSleep()
        sut.cancel(ResultCode.CANCELLED)

        for (i in 1..MAX_TRIES) {
            shortSleep()

            if (uploadResult != null) {
                break
            }
        }

        assertNotNull(uploadResult)
        TestCase.assertFalse(uploadResult?.isSuccess == true)
        TestCase.assertSame(ResultCode.CANCELLED, uploadResult?.code)
    }

    @Test
    fun resume() {
        val filePath = createFile("chunkedFile.txt", BIG_FILE_ITERATION * 2)
        val timestamp = System.currentTimeMillis() / MILLI_TO_SECOND
        val remotePath = "/bigFile.md"

        // set up first upload
        var sut = ChunkedFileUploadRemoteOperation(
            filePath,
            remotePath,
            "text/markdown",
            "",
            RANDOM_MTIME,
            timestamp,
            false,
            true
        )

        // start first upload
        var uploadResult: RemoteOperationResult<String>? = null
        Thread {
            uploadResult = sut.execute(client)
        }.start()

        // delay and cancel upload
        shortSleep()
        shortSleep()
        sut.cancel(ResultCode.CANCELLED)

        for (i in 1..MAX_TRIES) {
            shortSleep()

            if (uploadResult != null) {
                break
            }
        }

        // start second upload of same file
        sut = ChunkedFileUploadRemoteOperation(
            filePath,
            remotePath,
            "text/markdown",
            "",
            RANDOM_MTIME,
            timestamp,
            false,
            true
        )

        // reset result; start second upload
        uploadResult = null
        uploadResult = sut.execute(client)

        // second upload should succeed
        assert(uploadResult?.isSuccess == true)

        assert(File(filePath).length() == getRemoteSize(remotePath))
    }

    private fun genLargeUpload(onWifiConnection: Boolean): ChunkedFileUploadRemoteOperation {
        // create file
        val filePath = createFile("chunkedFile.txt", BIG_FILE_ITERATION)
        val remotePath = "/bigFile.md"

        return ChunkedFileUploadRemoteOperation(
            filePath,
            remotePath,
            "text/markdown",
            "",
            RANDOM_MTIME,
            System.currentTimeMillis() / MILLI_TO_SECOND,
            onWifiConnection,
            true
        )
    }

    private fun getRemoteSize(remotePath: String): Long {
        val davPath = client.filesDavUri.toString() + "/" + WebdavUtils.encodePath(remotePath)
        val propFindMethod = PropFindMethod(davPath, WebdavUtils.getFilePropSet(), 0)
        client.executeMethod(propFindMethod)
        assert(propFindMethod.succeeded())

        return WebdavEntry(propFindMethod.responseBodyAsMultiStatus.responses[0], remotePath).contentLength
    }

    companion object {
        const val BIG_FILE_ITERATION = 1000000
        const val MAX_TRIES = 30
    }
}
