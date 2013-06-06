/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.model;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILLayoutElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;
import org.w3c.dom.smil.SMILRegionElement;
import org.w3c.dom.smil.SMILRootLayoutElement;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.ContentRestrictionException;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.android.mms.layout.LayoutManager;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.android.mms.model.VcardModel;

public class SlideshowModel extends Model
        implements List<SlideModel>, IModelChangedObserver {
    private static final String TAG = "Mms/slideshow";

    private final LayoutModel mLayout;
    private final ArrayList<SlideModel> mSlides;
    private SMILDocument mDocumentCache;
    private PduBody mPduBodyCache;
    private static final int MMS_INIT_SIZE = 1 * 1024;
    private int mCurrentMessageSize=MMS_INIT_SIZE;    // This is the current message size, not including
                                        // attachments that can be resized (such as photos)
    private int mTotalMessageSize;      // This is the computed total message size
    private Context mContext;
    public final ArrayList<MediaModel> mMedia = new ArrayList<MediaModel>();

    // amount of space to leave in a slideshow for text and overhead.
    public static final int SLIDESHOW_SLOP = 1024;

    private SlideshowModel(Context context) {
        mLayout = new LayoutModel();
        mSlides = new ArrayList<SlideModel>();
        mContext = context;
    }

    private SlideshowModel (
            LayoutModel layouts, ArrayList<SlideModel> slides,
            SMILDocument documentCache, PduBody pbCache,
            Context context) {
        mLayout = layouts;
        mSlides = slides;
        mContext = context;

        mDocumentCache = documentCache;
        mPduBodyCache = pbCache;
        for (SlideModel slide : mSlides) {
            increaseMessageSize(slide.getSlideSize());
            slide.setParent(this);
        }
    }

    public static SlideshowModel createNew(Context context) {
        return new SlideshowModel(context);
    }

    public static SlideshowModel createFromMessageUri(
            Context context, Uri uri) throws MmsException {
        return createFromPduBody(context, getPduBody(context, uri));
    }
    public boolean isOnlySimpleAttach() {  
        // There must be one (and only one) slide.
        if (size() != 1)
            return false;       
        SlideModel slide = get(0);
        if (slide.hasImage() || slide.hasVideo() || slide.hasAudio()
            || slide.hasText())
        {
            return false;
        }        
        return true;
    }
    public boolean SimpleAttach() {  
        SlideModel slide = get(0);
        if ((size() == 1)&&(slide.hasImage() || slide.hasVideo() || slide.hasAudio()))
            return true;       
            return false;
    }

    public static SlideshowModel createFromPduBody(Context context, PduBody pb) throws MmsException {
        SMILDocument document = SmilHelper.getDocument(pb);

        // Create root-layout model.
        SMILLayoutElement sle = document.getLayout();
        SMILRootLayoutElement srle = sle.getRootLayout();
        int w = srle.getWidth();
        int h = srle.getHeight();
        if ((w == 0) || (h == 0)) {
            w = LayoutManager.getInstance().getLayoutParameters().getWidth();
            h = LayoutManager.getInstance().getLayoutParameters().getHeight();
            srle.setWidth(w);
            srle.setHeight(h);
        }
        RegionModel rootLayout = new RegionModel(
                null, 0, 0, w, h);

        // Create region models.
        ArrayList<RegionModel> regions = new ArrayList<RegionModel>();
        NodeList nlRegions = sle.getRegions();
        int regionsNum = nlRegions.getLength();

        for (int i = 0; i < regionsNum; i++) {
            SMILRegionElement sre = (SMILRegionElement) nlRegions.item(i);
            RegionModel r = new RegionModel(sre.getId(), sre.getFit(),
                    sre.getLeft(), sre.getTop(), sre.getWidth(), sre.getHeight(),
                    sre.getBackgroundColor());
            regions.add(r);
        }
        LayoutModel layouts = new LayoutModel(rootLayout, regions);

        // Create slide models.
        SMILElement docBody = document.getBody();
        NodeList slideNodes = docBody.getChildNodes();
        int slidesNum = slideNodes.getLength();
        ArrayList<SlideModel> slides = new ArrayList<SlideModel>(slidesNum);
        int totalMessageSize = 0;
        if(slidesNum<10){
                            MmsConfig.setMaxheadSize(MmsConfig.getHeadSize());
        }
        
        if(slidesNum>=10&&slidesNum<15){
                            MmsConfig.setMaxheadSize(MmsConfig.getHeadSize()*2);
        }
        if(slidesNum>=15){
                            MmsConfig.setMaxheadSize(MmsConfig.getHeadSize()*4);
        }
        for (int i = 0; i < slidesNum; i++) {
            // FIXME: This is NOT compatible with the SMILDocument which is
            // generated by some other mobile phones.
            SMILParElement par = (SMILParElement) slideNodes.item(i);

            // Create media models for each slide.
            NodeList mediaNodes = par.getChildNodes();
            int mediaNum = mediaNodes.getLength();
            ArrayList<MediaModel> mediaSet = new ArrayList<MediaModel>(mediaNum);

            for (int j = 0; j < mediaNum; j++) {
                //SMILMediaElement sme = (SMILMediaElement) mediaNodes.item(j);
                SMILMediaElement sme = null;
                Node node = (Node) mediaNodes.item(j);
                if(!(node instanceof SMILMediaElement)){  //Cindy modify for 
                    Log.v(TAG, "Cindy199 sme node is =" + node.getClass().getName());
                    continue;
                }else{
                    sme = (SMILMediaElement) node;
                }
                
                try {
                    MediaModel media = MediaModelFactory.getMediaModel(
                            context, sme, layouts, pb);
                    if(null == media){
                        continue;
                    }
                    SmilHelper.addMediaElementEventListeners(
                            (EventTarget) sme, media);
                    mediaSet.add(media);
                    totalMessageSize += media.getMediaSize();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            float   slide_duration =par.getDur();
            if(slide_duration==0)
               slide_duration=5;
            SlideModel slide = new SlideModel((int) (slide_duration * 1000), mediaSet);
            slide.setFill(par.getFill());
            SmilHelper.addParElementEventListeners((EventTarget) par, slide);
            slides.add(slide);
        }

        SlideshowModel slideshow = new SlideshowModel(layouts, slides, document, pb, context);
        slideshow.mTotalMessageSize = totalMessageSize;
        slideshow.registerModelChangedObserver(slideshow);
                                        
        int num = pb.getPartsNum();
        MediaModel vCardModel = null;
        MediaModel vCalModel = null;
        FileModel fileModel = null;
        PduPart part;
        for(int i = 0; i < num; i++){
            
            part = pb.getPart(i);
            Log.v(TAG,"partType  ="+new String(part.getContentType()));
            byte[] location = part.getName();
            
            if (location == null) {
                                location = part.getContentLocation();
            }
            
            if (location == null) {
                                location = part.getFilename();
            }                                                                                 
            if (null == location){
                                location = new String("Unknown").getBytes();
            }
                                                            
            if ( (new String(part.getContentType()).equals(ContentType.TEXT_VCARD))||((new String(location)).lastIndexOf (".vcf")==((new String(location)).length()-4)) ){
                Log.v(TAG,"vcard  ="+part.getData());
                
                if(part.getData()==null)
                                    continue;
                if (location.equals("Unknown")){
                                    location = new String("vcard.vcf").getBytes();
                }
                vCardModel = new VcardModel(context, ContentType.TEXT_VCARD, (new String(location)), part.getCharset(), part.getData());
                slideshow.addWithoutCheckSize(vCardModel);
            }else if(new String(part.getContentType()).equals("application/octet-stream")
                || new String(part.getContentType()).equals("application/oct-stream")
                || new String(part.getContentType()).equals("application/octet-stream")                
                || new String(part.getContentType()).equals("application/x-octet-stream")   
                || new String(part.getContentType()).equals("application/x-zip-compressed")   
                || new String(part.getContentType()).equals("application/vnd.android.package-archive")                  
                ){

                fileModel = new FileModel(context, part.getDataUri(), new String(location));                    
                    try{
                                slideshow.add(fileModel);
                    }catch(ExceedMessageSizeException e){

                                return slideshow;
                    }
            }
        }
        return slideshow;
    }

    public PduBody toPduBody() {
        if (mPduBodyCache == null) {
            mDocumentCache = SmilHelper.getDocument(this);
            mPduBodyCache = makePduBody(mDocumentCache);
        }
        return mPduBodyCache;
    }

    
    private PduBody makePduBody(SMILDocument document) {
        PduBody pb = new PduBody();

        boolean hasForwardLock = false;
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                PduPart part = new PduPart();

                if (media.isText()) {
                    TextModel text = (TextModel) media;
                    // Don't create empty text part.
                    if (TextUtils.isEmpty(text.getText())) {
                        continue;
                    }
                    // Set Charset if it's a text media.
                    part.setCharset(text.getCharset());
                }

                // Set Content-Type.
                part.setContentType(media.getContentType().getBytes());

                String src = media.getSrc();
                String location;
                boolean startWithContentId = src.startsWith("cid:");
                if (startWithContentId) {
                    location = src.substring("cid:".length());
                } else {
                    location = src;
                }

                // Set Content-Location.
                part.setContentLocation(location.getBytes());

                // Set Content-Id.
                if (startWithContentId) {
                    //Keep the original Content-Id.
                    part.setContentId(location.getBytes());
                }
                else {
                    int index = location.lastIndexOf(".");
                    String contentId = (index == -1) ? location
                            : location.substring(0, index);
                    part.setContentId(contentId.getBytes());
                }

                if (media.isText()) {
                    part.setData(((TextModel) media).getText().getBytes());
                } else if (media.isImage() || media.isVideo() || media.isAudio()|| media.isVcard()) {
                    part.setDataUri(media.getUri());
					/*
                    if (media.isVcard() && !TextUtils.isEmpty(((VcardModel) media).getLookupUri())) {
                        part.setContentDisposition(((VcardModel) media).getLookupUri().getBytes());
                    }
                    */
                } else {
                    Log.w(TAG, "Unsupport media: " + media);
                }

                pb.addPart(part);
            }
        }
        
        for(MediaModel media : mMedia){
    
                PduPart part = new PduPart();
    
                // Set Content-Type.
                part.setContentType(media.getContentType().getBytes());
    
                String src = media.getSrc();
                String location;
                boolean startWithContentId = src.startsWith("cid:");
                if (startWithContentId) {
                    location = src.substring("cid:".length());
                } else {
                    location = src;
                }
    
                // Set Content-Location.
                part.setContentLocation(location.getBytes());
    
                // Set Content-Id.
                if (startWithContentId) {
                    //Keep the original Content-Id.
                    part.setContentId(location.getBytes());
                }
                else {
                    int index = location.lastIndexOf(".");
                    String contentId = (index == -1) ? location
                                : location.substring(0, index);
                    part.setContentId(contentId.getBytes());
                }
    
                if (media.isVcard()) {
                    VcardModel text = (VcardModel) media;
                    if (TextUtils.isEmpty(text.getText())) {
                        continue;
                    }
                    part.setData(((VcardModel) media).getText().getBytes());
                    part.setCharset(text.getCharset());
                } else if (media.isFile()) {
                    FileModel file = (FileModel) media;
                    if (file.getUri() == null) {
                        continue;
                    }
                    Log.v(TAG, "__________file.getUri()   ==  "+file.getUri()+"_____getAuthority()  ==  "+file.getUri().getScheme());
                    part.setDataUri(file.getUri());
                } else {
                    Log.w(TAG, "Unsupport media: " + media);
                }
    
                pb.addPart(part);
            }

        // Create and insert SMIL part(as the first part) into the PduBody.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(document, out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pb.addPart(0, smilPart);

        return pb;
    }
    public boolean addWithoutCheckSize(MediaModel object) {
        mediaNameCheck(object);
        int increaseSize = object.getMediaResizable() ? 0 : object.getMediaSize();;

        if ((object != null) && mMedia.add(object)) {
            increaseMessageSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
            return true;
        }
        return false;
    }
    private void mediaNameCheck(MediaModel m){
        if (mSlides != null){
            String name = m.getSrc();
            String s;
            int i = 0;
            
            boolean exist = false;

            if (null == name){
                return;
            }

            int index_point = name.indexOf(".");
            if (index_point < 0){
                                    Log.w(TAG,"name is weird, and it has no suffix");
                                    return;
            }
            String first = name.substring(0, index_point);
            String last = name.substring(index_point);
            Log.w(TAG,"first"+first+"  "+last);
            if (0 == index_point ){
            
                name = "emptyname"+ last;
                m.setSrc(name);
                return;
            }
            
            while (true){
                for (SlideModel sm : mSlides){
                    for (MediaModel tmp : sm){

                        if (tmp == m){
                            continue;
                        }
                        
                        s = tmp.getSrc();
                         
                        if (name.equals(s)){
                            exist = true;
                            break;
                        }
                    }
                }

                if(!exist){
                    for (MediaModel tmp : mMedia){

                        if (tmp == m){
                            continue;
                        }
                        
                        s = tmp.getSrc();
                         
                        if (name.equals(s)){
                            exist = true;
                            break;
                        }
                    }
                }

                if (exist){
                    int index = name.indexOf(".");
                    if (index < 0){
                        Log.w(TAG,"name is weird, and it has no suffix");
                        return;
                    }

                    String one = name.substring(0, index);
                    String two = name.substring(index);
                    
                    name = one + "_" + i + two;
                    
                    i++;
                    exist = false;
                    Log.w(TAG, "the name exist, try to use new name: " + name);
                }else{
                    //Log.w(TAG, "use new name " + name + " to replace " + m.getSrc());
                    m.setSrc(name);
                    break;
                }
            }
        }
    } 

    public HashMap<Uri, InputStream> openPartFiles(ContentResolver cr) {
        HashMap<Uri, InputStream> openedFiles = null;     // Don't create unless we have to

        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                if (media.isText()) {
                    continue;
                }
                Uri uri = media.getUri();
                InputStream is;
                try {
                    is = cr.openInputStream(uri);
                    if (is != null) {
                        if (openedFiles == null) {
                            openedFiles = new HashMap<Uri, InputStream>();
                        }
                        openedFiles.put(uri, is);
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "openPartFiles couldn't open: " + uri, e);
                }
            }
        }
        return openedFiles;
    }

    public PduBody makeCopy() {
        return makePduBody(SmilHelper.getDocument(this));
    }

    public SMILDocument toSmilDocument() {
        if (mDocumentCache == null) {
            mDocumentCache = SmilHelper.getDocument(this);
        }
        return mDocumentCache;
    }

    public static PduBody getPduBody(Context context, Uri msg) throws MmsException {
        PduPersister p = PduPersister.getPduPersister(context);
        GenericPdu pdu = p.load(msg);

        int msgType = pdu.getMessageType();
        if ((msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ)
                || (msgType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)) {
            return ((MultimediaMessagePdu) pdu).getBody();
        } else {
            throw new MmsException();
        }
    }

    public boolean hasOctstream(){
        return mMedia.size() > 0;
    }
    public void setCurrentMessageSize(int size) {
        mCurrentMessageSize = size;
    }

    // getCurrentMessageSize returns the size of the message, not including resizable attachments
    // such as photos. mCurrentMessageSize is used when adding/deleting/replacing non-resizable
    // attachments (movies, sounds, etc) in order to compute how much size is left in the message.
    // The difference between mCurrentMessageSize and the maxSize allowed for a message is then
    // divided up between the remaining resizable attachments. While this function is public,
    // it is only used internally between various MMS classes. If the UI wants to know the
    // size of a MMS message, it should call getTotalMessageSize() instead.
    public int getCurrentMessageSize() {
        return mCurrentMessageSize;
    }

    // getTotalMessageSize returns the total size of the message, including resizable attachments
    // such as photos. This function is intended to be used by the UI for displaying the size of the
    // MMS message.
    public int getTotalMessageSize() {
        return mTotalMessageSize;
    }

    public void increaseMessageSize(int increaseSize) {
        if (increaseSize > 0) {
            mCurrentMessageSize += increaseSize;
        }
    }

    public void decreaseMessageSize(int decreaseSize) {
        if (decreaseSize > 0) {
            mCurrentMessageSize -= decreaseSize;
        }
    }

    public LayoutModel getLayout() {
        return mLayout;
    }

    //
    // Implement List<E> interface.
    //
    public boolean add(SlideModel object) {
        int increaseSize = object.getSlideSize();
        checkMessageSize(increaseSize);

        if ((object != null) && mSlides.add(object)) {
            increaseMessageSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
            return true;
        }
        return false;
    }
    

    public boolean add(MediaModel object) {
        mediaNameCheck(object);
        int increaseSize = object.getMediaResizable() ? 0 : object.getMediaSize();;
        checkMessageSize(increaseSize);

        if ((object != null) && mMedia.add(object)) {
            increaseMessageSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public boolean addAll(Collection<? extends SlideModel> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public void clear() {
        if (mSlides.size() > 0) {
            for (SlideModel slide : mSlides) {
                slide.unregisterModelChangedObserver(this);
                for (IModelChangedObserver observer : mModelChangedObservers) {
                    slide.unregisterModelChangedObserver(observer);
                }
            }
            mCurrentMessageSize = 1024;
            mSlides.clear();
            notifyModelChanged(true);
        }
    }

    public boolean contains(Object object) {
        return mSlides.contains(object);
    }

    public boolean containsAll(Collection<?> collection) {
        return mSlides.containsAll(collection);
    }

    public boolean isEmpty() {
        return mSlides.isEmpty();
    }

    public Iterator<SlideModel> iterator() {
        return mSlides.iterator();
    }

    public boolean remove(Object object) {
        if ((object != null) && mSlides.remove(object)) {
            SlideModel slide = (SlideModel) object;
            decreaseMessageSize(slide.getSlideSize());
            slide.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public int size() {
        return mSlides.size();
    }

    public Object[] toArray() {
        return mSlides.toArray();
    }

    public <T> T[] toArray(T[] array) {
        return mSlides.toArray(array);
    }

    public void add(int location, SlideModel object) {
        if (object != null) {
            int increaseSize = object.getSlideSize();
            checkMessageSize(increaseSize);

            mSlides.add(location, object);
            increaseMessageSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
        }
    }

    public boolean addAll(int location,
            Collection<? extends SlideModel> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public SlideModel get(int location) {
        return (location >= 0 && location < mSlides.size()) ? mSlides.get(location) : null;
    }

    public int indexOf(Object object) {
        return mSlides.indexOf(object);
    }

    public int lastIndexOf(Object object) {
        return mSlides.lastIndexOf(object);
    }

    public ListIterator<SlideModel> listIterator() {
        return mSlides.listIterator();
    }

    public ListIterator<SlideModel> listIterator(int location) {
        return mSlides.listIterator(location);
    }

    public SlideModel remove(int location) {
        SlideModel slide = mSlides.remove(location);
        if (slide != null) {
            decreaseMessageSize(slide.getSlideSize());
            slide.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
        }
        return slide;
    }

    public SlideModel set(int location, SlideModel object) {
        SlideModel slide = mSlides.get(location);
        if (null != object) {
            int removeSize = 0;
            int addSize = object.getSlideSize();
            if (null != slide) {
                removeSize = slide.getSlideSize();
            }
            if (addSize > removeSize) {
                checkMessageSize(addSize - removeSize);
                increaseMessageSize(addSize - removeSize);
            } else {
                decreaseMessageSize(removeSize - addSize);
            }
        }

        slide =  mSlides.set(location, object);
        if (slide != null) {
            slide.unregisterAllModelChangedObservers();
        }

        if (object != null) {
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
        }

        notifyModelChanged(true);
        return slide;
    }

    public List<SlideModel> subList(int start, int end) {
        return mSlides.subList(start, end);
    }

    @Override
    protected void registerModelChangedObserverInDescendants(
            IModelChangedObserver observer) {
        mLayout.registerModelChangedObserver(observer);

        for (SlideModel slide : mSlides) {
            slide.registerModelChangedObserver(observer);
        }
    }

    @Override
    protected void unregisterModelChangedObserverInDescendants(
            IModelChangedObserver observer) {
        mLayout.unregisterModelChangedObserver(observer);

        for (SlideModel slide : mSlides) {
            slide.unregisterModelChangedObserver(observer);
        }
    }

    @Override
    protected void unregisterAllModelChangedObserversInDescendants() {
        mLayout.unregisterAllModelChangedObservers();

        for (SlideModel slide : mSlides) {
            slide.unregisterAllModelChangedObservers();
        }
    }

    public void onModelChanged(Model model, boolean dataChanged) {
        if (dataChanged) {
            mDocumentCache = null;
            mPduBodyCache = null;
        
            mCurrentMessageSize = MMS_INIT_SIZE;
            for (SlideModel slide : mSlides) {
                for (MediaModel m : slide){
                    mCurrentMessageSize += m.getMediaSize();
                }
            }

            for (MediaModel m : mMedia) {
                mCurrentMessageSize += m.getMediaSize();
            }
        }
    }

    public void sync(PduBody pb) {
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                PduPart part = pb.getPartByContentLocation(media.getSrc());
                if (part != null) {
                    media.setUri(part.getDataUri());
                }
            }
        }
        for(MediaModel media : mMedia){
            PduPart part = pb.getPartByContentLocation(media.getSrc());
            if (part != null) {
                media.setUri(part.getDataUri());
            }
        }
    }

    public void checkMessageSize(int increaseSize) throws ContentRestrictionException {
        ContentRestriction cr = ContentRestrictionFactory.getContentRestriction();
        cr.checkMessageSize(mCurrentMessageSize, increaseSize, mContext.getContentResolver());
    }

    /**
     * Determines whether this is a "simple" slideshow.
     * Criteria:
     * - Exactly one slide
     * - Exactly one multimedia attachment, but no audio
     * - It can optionally have a caption
    */
    public boolean isSimple() {
        // There must be one (and only one) slide.
        if (size() != 1)
            return false;

        SlideModel slide = get(0);
        // The slide must have either an image or video, but not both.
        if (!(slide.hasImage() ^ slide.hasVideo()^slide.hasVcard()))
            return false;
     
        // No audio allowed.
        if (slide.hasAudio())
            return false;
        
        return true;
    }

    /**
     * Make sure the text in slide 0 is no longer holding onto a reference to the text
     * in the message text box.
     */
    public void prepareForSend() {
        if (size() == 1) {
            TextModel text = get(0).getText();
            if (text != null) {
                text.cloneText();
            }
        }
    }

    /**
     * Resize all the resizeable media objects to fit in the remaining size of the slideshow.
     * This should be called off of the UI thread.
     *
     * @throws MmsException, ExceedMessageSizeException
     */
    public void finalResize(Uri messageUri) throws MmsException, ExceedMessageSizeException {

        // Figure out if we have any media items that need to be resized and total up the
        // sizes of the items that can't be resized.
        int resizableCnt = 0;
        int fixedSizeTotal = 0;
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                if (media.getMediaResizable()) {
                    ++resizableCnt;
                } else {
                    fixedSizeTotal += media.getMediaSize();
                }
            }
        }
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.v(TAG, "finalResize: original message size: " + getCurrentMessageSize() +
                    " getMaxMessageSize: " + MmsConfig.getMaxMessageSize() +
                    " fixedSizeTotal: " + fixedSizeTotal);
        }
        if (resizableCnt > 0) {
            int remainingSize = MmsConfig.getMaxMessageSize() - fixedSizeTotal - SLIDESHOW_SLOP;
            if (remainingSize <= 0) {
                throw new ExceedMessageSizeException("No room for pictures");
            }
            long messageId = ContentUris.parseId(messageUri);
            int bytesPerMediaItem = remainingSize / resizableCnt;
            // Resize the resizable media items to fit within their byte limit.
            for (SlideModel slide : mSlides) {
                for (MediaModel media : slide) {
                    if (media.getMediaResizable()) {
                        media.resizeMedia(bytesPerMediaItem, messageId);
                    }
                }
            }
            // One last time through to calc the real message size.
            int totalSize = 0;
            for (SlideModel slide : mSlides) {
                for (MediaModel media : slide) {
                    totalSize += media.getMediaSize();
                }
            }
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.v(TAG, "finalResize: new message size: " + totalSize);
            }

            if (totalSize > MmsConfig.getMaxMessageSize()) {
                throw new ExceedMessageSizeException("After compressing pictures, message too big");
            }
            setCurrentMessageSize(totalSize);

            onModelChanged(this, true);     // clear the cached pdu body
            PduBody pb = toPduBody();
            // This will write out all the new parts to:
            //      /data/data/com.android.providers.telephony/app_parts
            // and at the same time delete the old parts.
            PduPersister.getPduPersister(mContext).updateParts(messageUri, pb, null);
        }
    }
    public boolean isSimpleAttach() {  
        // There must be one (and only one) slide.
        if (size() != 1)
            return false;       
        SlideModel slide = get(0);
        // The slide must have either an image or video, but not both.
        if (!(slide.hasImage() ^ slide.hasVideo()))
            return false;
     
        // No audio allowed.
        if (slide.hasAudio())
            return false;
        
        return true;
    } 
    

}
