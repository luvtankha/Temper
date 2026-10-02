await Promise.all(['Regular','Bold'].map(style=>figma.loadFontAsync({family:'Roboto',style})));
const page=await figma.getNodeByIdAsync(PAGE_ID);await figma.setCurrentPageAsync(page);page.name=PAGE_NAME;
const made=[],mutated=[];const vars=Object.fromEntries((await figma.variables.getLocalVariablesAsync()).map(v=>[v.name,v]));
const rgb=hex=>({r:parseInt(hex.slice(1,3),16)/255,g:parseInt(hex.slice(3,5),16)/255,b:parseInt(hex.slice(5,7),16)/255});
const fill=(n,name)=>n.fills=[figma.variables.setBoundVariableForPaint({type:'SOLID',color:rgb(TOKENS.colors[name])},'color',vars['color/'+name])];
const track=n=>{made.push(n.id);return n;};
const all=n=>[n.id,...('children'in n?n.children.flatMap(all):[])];
function txt(value,size=14,color='text',bold=false){const n=track(figma.createText());n.fontName={family:'Roboto',style:bold?'Bold':'Regular'};n.fontSize=size;n.lineHeight={unit:'PIXELS',value:Math.round(size*1.35)};n.characters=value;fill(n,color);return n;}
function frame(name,w,h,layout=null){const n=track(figma.createFrame());n.name=name;n.resize(w,h);n.fills=[];if(layout){n.layoutMode=layout;n.primaryAxisSizingMode='FIXED';n.counterAxisSizingMode='FIXED';}return n;}
const c={};for(const[k,v]of Object.entries(LEDGER.components))if(!Array.isArray(v))c[k]=await figma.getNodeByIdAsync(v);
c.status=await Promise.all(LEDGER.components.status.map(id=>figma.getNodeByIdAsync(id)));c.toggle=await Promise.all(LEDGER.components.toggle.map(id=>figma.getNodeByIdAsync(id)));c.item=await Promise.all(LEDGER.components.item.map(id=>figma.getNodeByIdAsync(id)));c.carousel=await Promise.all(LEDGER.components.carousel.map(id=>figma.getNodeByIdAsync(id)));
const arts={};for(const[k,v]of Object.entries(LEDGER.arts))arts[k]=await figma.getNodeByIdAsync(v);
function inst(comp,parent){const n=comp.createInstance();parent.appendChild(n);made.push(...all(n));return n;}
function swapProps(instance,id,name,subtitle){const props=instance.componentProperties;const values={};for(const k of Object.keys(props)){if(k.startsWith('AvatarImage#'))values[k]=arts[id].id;if(k.startsWith('AvatarName#'))values[k]=name;if(k.startsWith('Subtitle#'))values[k]=subtitle;}instance.setProperties(values);}
const catalog=[{id:'studio_nova',name:'Nova',subtitle:'Calm and observant'},{id:'kai',name:'Kai',subtitle:'Focused and adaptive'},{id:'astra',name:'Astra',subtitle:'Thoughtful and grounded'},{id:'mira',name:'Mira',subtitle:'Analytical and intuitive'},{id:'volt',name:'Volt',subtitle:'Energetic and expressive'}];
const screens=[],controls=[];
const cases=MODE==='prototype'?catalog.flatMap((a,index)=>[{index,on:false},{index,on:true}]):[{index:2,on:false},{index:2,on:true},{index:3,on:true},{index:1,on:false}];
const heading=txt(MODE==='prototype'?'Swipe, select, activate':'Choose a companion. One switch.',32,'text',true);page.appendChild(heading);heading.x=0;heading.y=-100;
const intro=txt(MODE==='prototype'?'Explore forward and reverse · 300 ms Smart Animate · activation preserves selection':'TEMPER / Android home · 390 × 844 · original 2D artwork',14,'secondary');page.appendChild(intro);intro.x=0;intro.y=-48;
for(let i=0;i<cases.length;i++){
    const state=cases[i],avatar=catalog[state.index];const screen=frame(avatar.name+' / '+(state.on?'ON':'OFF'),390,844,'VERTICAL');screen.itemSpacing=0;screen.paddingLeft=20;screen.paddingRight=20;screen.paddingTop=16;screen.paddingBottom=20;fill(screen,'background');screen.cornerRadius=24;screen.clipsContent=true;screen.x=(i%5)*450;screen.y=Math.floor(i/5)*960;page.appendChild(screen);
    const androidStatus=frame('Android system bar',350,24,'HORIZONTAL');screen.appendChild(androidStatus);androidStatus.layoutSizingHorizontal='FILL';androidStatus.primaryAxisAlignItems='SPACE_BETWEEN';androidStatus.appendChild(txt('9:41',11,'secondary',true));androidStatus.appendChild(txt('●  ▰',11,'secondary'));const header=inst(c.header,screen);header.layoutSizingHorizontal='FILL';const nestedStatus=header.children.find(n=>n.type==='INSTANCE');if(nestedStatus)nestedStatus.swapComponent(c.status[state.on?1:0]);
    const strip=inst(c.carousel[state.index],screen);strip.layoutSizingHorizontal='FILL';strip.name='Avatar carousel';
    const slots=strip.children.filter(n=>n.type==='INSTANCE');
    const nav=frame('Carousel navigation',350,48,'HORIZONTAL');screen.appendChild(nav);nav.layoutSizingHorizontal='FILL';nav.primaryAxisAlignItems='SPACE_BETWEEN';nav.counterAxisAlignItems='CENTER';const buttons=[];
    for(const direction of [-1,0,1]){if(!direction){nav.appendChild(txt('Swipe to explore',12,'muted'));continue;}const b=frame(direction<0?'Previous avatar':'Next avatar',48,48);nav.appendChild(b);const circle=track(figma.createEllipse());circle.resize(34,34);fill(circle,'surface');b.appendChild(circle);circle.x=7;circle.y=7;const svg=figma.createNodeFromSvg('<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 18 18"><path d="'+(direction<0?'M11 4 L6 9 L11 14':'M7 4 L12 9 L7 14')+'" fill="none" stroke="#b9bdce" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>');b.appendChild(svg);made.push(...all(svg));svg.x=15;svg.y=15;buttons.push(b);}
    const hero=inst(c.hero,screen);hero.layoutSizingHorizontal='FILL';swapProps(hero,avatar.id,avatar.name,avatar.subtitle);hero.name='Selected avatar';hero.opacity=state.on?1:.8;const stage=hero.children.find(n=>n.name==='Stage');const aura=stage.children.find(n=>n.name==='Restrained aura');aura.opacity=state.on?.16:.05;
    const spacer=frame('Activation spacing',350,16);screen.appendChild(spacer);const instruction=txt('Show Temper avatar on chat apps',14);screen.appendChild(instruction);instruction.textAlignHorizontal='CENTER';instruction.layoutSizingHorizontal='FILL';
    const switchRow=frame('Master power',350,60,'HORIZONTAL');screen.appendChild(switchRow);switchRow.layoutSizingHorizontal='FILL';switchRow.primaryAxisAlignItems='CENTER';const toggle=inst(c.toggle[state.on?1:0],switchRow);const status=txt(state.on?'Currently active':'Currently disabled',12,state.on?'active':'muted');screen.appendChild(status);status.layoutSizingHorizontal='FILL';status.textAlignHorizontal='CENTER';const infoSpace=frame('Preview spacing',350,20);screen.appendChild(infoSpace);const info=inst(c.info,screen);info.layoutSizingHorizontal='FILL';swapProps(info,avatar.id,avatar.name);
    screens.push(screen);controls.push({strip,buttons,toggle,index:state.index,on:state.on,slots});
    const label=txt(screen.name,16,'secondary',true);page.appendChild(label);label.x=screen.x;label.y=screen.y+870;
}
const transition={type:'SMART_ANIMATE',easing:{type:'CUSTOM_CUBIC_BEZIER',easingFunctionCubicBezier:{x1:.2,y1:0,x2:.2,y2:1}},duration:.3};
if(MODE==='prototype'){
    const link=async(node,destination,trigger='ON_CLICK')=>{let root=node;while(root.parent&&root.parent.type!=='PAGE')root=root.parent;if(root.id===destination.id)return;await node.setReactionsAsync([{trigger:{type:trigger},actions:[{type:'NODE',destinationId:destination.id,navigation:'NAVIGATE',transition,resetScrollPosition:true}]}]);};
    for(let i=0;i<controls.length;i++){const p=controls[i],same=index=>screens.find((s,j)=>controls[j].index===index&&controls[j].on===p.on);await link(p.buttons[0],same(Math.max(0,p.index-1)));await link(p.buttons[1],same(Math.min(4,p.index+1)));await link(p.strip,same(Math.min(4,p.index+1)),'ON_DRAG');await link(p.toggle,screens.find((s,j)=>controls[j].index===p.index&&controls[j].on!==p.on));for(let j=0;j<p.slots.length;j++)await link(p.slots[j],same(j));}
    page.flowStartingPoints=[{nodeId:screens[4].id,name:'TEMPER · Astra OFF'}];
}
figma.viewport.scrollAndZoomIntoView([screens[MODE==='prototype'?4:0]]);
return {createdRootNodeIds:page.children.map(n=>n.id),createdNodeIds:made.filter(id=>!id.startsWith('I')),createdDescendantIdsByRoot:screens.map(n=>({rootId:n.id,descendantCount:all(n).length})),mutatedNodeIds:[page.id],screens:screens.map(n=>({id:n.id,name:n.name,width:n.width,height:n.height})),pageId:page.id};
