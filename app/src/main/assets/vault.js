/* Folder Locker native vault bridge + browser fallback. */
const CATEGORY_RULES={
 images:{label:'Images',icon:'🖼️',accept:'.jpg,.jpeg,.png,.gif,.webp,.bmp'},
 videos:{label:'Videos',icon:'🎬',accept:'video/*,.mp4,.mov,.mkv,.webm,.3gp'},
 music:{label:'Music',icon:'🎵',accept:'audio/*,.mp3,.wav,.m4a,.ogg,.aac'},
 documents:{label:'Documents',icon:'📄',accept:'.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv'}
};
function isNative(){return !!window.AndroidVault;}
function mimeFor(n){let x=n.toLowerCase();if(/\.(jpg|jpeg|png|gif|webp|bmp)$/.test(x))return'image/*';if(/\.(mp4|mov|mkv|webm|3gp)$/.test(x))return'video/*';if(/\.(mp3|wav|m4a|ogg|aac)$/.test(x))return'audio/*';return'application/octet-stream';}
function browserFiles(category){try{return JSON.parse(localStorage.getItem('FL_'+category)||'[]')}catch(e){return[]}}
function saveBrowserFiles(category,a){localStorage.setItem('FL_'+category,JSON.stringify(a));}
function listVault(category){if(isNative()){try{return JSON.parse(AndroidVault.listFiles(category)||'[]')}catch(e){return[]}}return browserFiles(category);}
function pickVault(category){if(isNative()){AndroidVault.pickFiles(category);return;}const input=document.getElementById('nativePicker');input.accept=CATEGORY_RULES[category].accept;input.click();}
function onNativeFilesImported(category,count){loadCategory();if(count) toast(count+' file(s) added to '+CATEGORY_RULES[category].label,'success');else toast('No compatible files were added','error');}
function browserPick(category,files){let a=browserFiles(category);for(const f of files){if(!accepts(category,f))continue;const r=new FileReader();r.onload=e=>{a.push({name:f.name,size:f.size,time:Date.now(),mime:f.type||mimeFor(f.name),data:e.target.result});saveBrowserFiles(category,a);loadCategory();};r.readAsDataURL(f);} }
function accepts(c,f){return c==='images'?f.type.startsWith('image/'):c==='videos'?f.type.startsWith('video/'):c==='music'?f.type.startsWith('audio/'):(/\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|csv)$/i.test(f.name));}
function deleteVault(category,name){if(isNative()){const ok=AndroidVault.deleteFile(category,name);loadCategory();return ok;}let a=browserFiles(category).filter(x=>x.name!==name);saveBrowserFiles(category,a);loadCategory();return true;}
function renameVault(category,oldName,newName){if(isNative()){const ok=AndroidVault.renameFile(category,oldName,newName);loadCategory();return ok;}let a=browserFiles(category);let f=a.find(x=>x.name===oldName);if(!f||a.some(x=>x.name===newName))return false;f.name=newName;saveBrowserFiles(category,a);loadCategory();return true;}
function fileUrl(category,f){return f.url||f.data;}
