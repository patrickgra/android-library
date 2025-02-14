/* Nextcloud Android Library is available under MIT license
 *
 *   @author Tobias Kaminsky
 *   Copyright (C) 2018 Tobias Kaminsky
 *   Copyright (C) 2018 Nextcloud GmbH
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.trashbin;

import android.util.Log;

import com.nextcloud.common.NextcloudClient;
import com.nextcloud.operations.DeleteMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;


/**
 * Empty trashbin.
 */
public class EmptyTrashbinRemoteOperation extends RemoteOperation<Boolean> {

    private static final String TAG = EmptyTrashbinRemoteOperation.class.getSimpleName();

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote Nextcloud server.
     */
    @Override
    public RemoteOperationResult<Boolean> run(NextcloudClient client) {

        DeleteMethod delete = null;
        RemoteOperationResult<Boolean> result;
        try {
            delete = new DeleteMethod(
                    client.getDavUri() + "/trashbin/" + client.getUserId() + "/trash",
                    true);
            int status = client.execute(delete);

            result = new RemoteOperationResult<>(isSuccess(status), delete);
        } catch (IOException e) {
            result = new RemoteOperationResult<>(e);
            Log.e(TAG, "Empty trashbin failed: " + result.getLogMessage(), e);
        } finally {
            if (delete != null) {
                delete.releaseConnection();
            }
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_NO_CONTENT;
    }
}
