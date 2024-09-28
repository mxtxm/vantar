package com.vantar.util.upload;

import com.vantar.business.ModelCommon;
import com.vantar.common.*;
import com.vantar.database.common.Db;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.util.upload.dto.*;
import com.vantar.web.*;
import java.util.*;


public class UploadUtil {

    public static Result addAttachments(Params params, AttachmentDto dto, AttachmentRule rule) throws VantarException {
        Result result = new Result();
        addAttachmentItem(params, dto, rule, result);
        return result;
    }

    /**
     * dto.id=null --> cache
     *  Rules:
     *      key=file-key
     *      each rule item:
     *          has a tag and one type of files with the same rules,
     *          for example images would require their own tag and rules, videos would require their own tag and rules
     */
    public static Result addAttachments(Params params, AttachmentDto dto, Set<AttachmentRule> rules) throws VantarException {
        return (Result) ModelCommon.mutex(dto, dummy -> {
            Result result = new Result();
            for (AttachmentRule rule : rules) {
                addAttachmentItem(params, dto, rule, result);
            }
            return result;
        });
    }

    private static void addAttachmentItem(Params params, AttachmentDto dto, AttachmentRule rule, Result result) throws VantarException {
        CommonUser user = params.getCurrentUser();
        try (Params.Uploaded uploaded = params.upload(rule.key)) {
            if (!uploaded.isUploaded()) {
                return;
            }

            // check IO errors
            if (!uploaded.isUploadedOk()) {
                result.addFail(Locale.getString(uploaded.getError(), rule.key));
                return;
            }

            // check type
            boolean mediaTypeIsCorrect = false;
            List<String> allowedTypes = new ArrayList<>(10);
            for (MediaType mediaType : rule.mediaTypes) {
                String fileExt = mediaType.name().toLowerCase();
                if (uploaded.isType(fileExt)) {
                    mediaTypeIsCorrect = true;
                    break;
                }
                allowedTypes.add(mediaType.name());
            }
            if (!mediaTypeIsCorrect) {
                result.addFail(Locale.getString(VantarKey.FILE_TYPE, rule.key, CollectionUtil.join(allowedTypes, ", ")));
                return;
            }

            // check size
            long filesize = uploaded.getSize();
            if (filesize < rule.getMinSizeBytes() || filesize > rule.getMaxSizeBytes()) {
                result.addFail(Locale.getString(VantarKey.FILE_SIZE, rule.key, rule.minSize + " ~ " + rule.maxSize));
                return;
            }

            Attachment attachment = new Attachment();
            attachment.dateTime = new DateTime();
            attachment.comments = params.getString("comments");
            attachment.location = params.getLocation("location");
            attachment.userId = user.getId();
            attachment.username = user.getUsername();
            attachment.userFullName = user.getFullName();
            attachment.tag = rule.tag;
            attachment.originalFilename = uploaded.getOriginalFilename();

            long fileId;
            try {
                fileId = Db.mongo.autoIncrementGetNext("fileName");
            } catch (VantarException e) {
                result.addFail(Locale.getString(VantarKey.FAIL_UPLOAD, rule.key));
                return;
            }
            String dtoDir = dto.getClass().getSimpleName().toLowerCase();
            attachment.path = dtoDir + "/";
            Long dtoId = dto.getId();
            if (rule.path == null) {
                attachment.path += (dtoId == null ? "XXXIDXXX" : dtoId);
            } else {
                attachment.path += StringUtil.trim(rule.path, '/');
            }
            attachment.path +=  "/" + fileId + "." + uploaded.getOriginalExtension().toLowerCase();

            if (!uploaded.moveTo(Settings.config.getProperty("upload.dir") + attachment.path)) {
                result.addFail(Locale.getString(VantarKey.FAIL_UPLOAD, rule.key));
                return;
            }

            if (dtoId == null) {
                AttachmentCache cache = new AttachmentCache();
                cache.attachment = attachment;
                result.cacheId = Db.mongo.insert(cache);
            } else {
                List<Attachment> attachments = dto.getAttachments();
                if (attachments == null) {
                    attachments = new ArrayList<>(5);
                } else {
                    attachments.removeIf(item -> attachment.path.equals(item.path));
                    if (!rule.multi) {
                        attachments.removeIf(item -> rule.tag != null && rule.tag.equals(item.tag));
                    }
                }
                attachments.add(attachment);
                dto.setAttachments(attachments);
            }
            result.addSuccess(Locale.getString(VantarKey.SUCCESS_UPLOAD, rule.key));
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public static void addAttachmentsFromCache(Params params, AttachmentDto dto) {
        List<Long> uploadIds =
            Params.Type.JSON.equals(params.type) ?
                params.getLongList("uploadIds") :
                params.getJson(JsonUploadIds.class).uploadIds;
        if (ObjectUtil.isEmpty(uploadIds)) {
            return;
        }

        List<Attachment> attachmentsX = dto.getAttachments();
        if (attachmentsX == null) {
            attachmentsX = new ArrayList<>(5);
        }
        List<Attachment> attachments = attachmentsX;

        QueryBuilder q = new QueryBuilder(new AttachmentCache());
        q.condition().inNumber("id", uploadIds);
        try {
            Db.mongo.getData(q).forEach(cacheX -> {
                AttachmentCache cache = (AttachmentCache) cacheX;
                attachments.removeIf(item -> cache.attachment.path.equals(item.path));
                return true;
            });
            Db.mongo.delete(q);
        } catch (Exception e) {
            ServiceLog.log.error("! upload from cache ({}.{}) <- {}", dto.getClass().getSimpleName(), dto.getId(), uploadIds, e);
        }
    }


    private static class JsonUploadIds {
        public List<Long> uploadIds;
    }


    public static class Result {

        public Long cacheId;
        public int uploaded;
        public int failed;
        public List<String> messages;

        public void addFail(String msg) {
            ++failed;
            if (messages == null) {
                messages = new ArrayList<>(5);
                messages.add(msg);
            }
        }

        public void addSuccess(String msg) {
            ++uploaded;
            if (messages == null) {
                messages = new ArrayList<>(5);
                messages.add(msg);
            }
        }

        @Override
        public String toString() {
            return ObjectUtil.toString(this);
        }
    }


    public interface AttachmentDto extends Dto {

        // <path, data>
        List<Attachment> getAttachments();
        void setAttachments(List<Attachment> attachments);
    }
}
