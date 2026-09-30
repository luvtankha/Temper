import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import svgpath from 'svgpath';

// Original TEMPER vectors become editable Rive geometry, not embedded images.
// Run from frontend: node scripts/build-rig-source.mjs, then compile with Rive CLI.
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
let counter=1;
const id=()=>`0:${counter++}`;
const n=v=>Number(v.toFixed(5));
const attr=o=>Object.entries(o).map(([k,v])=>`${k}="${v}"`).join(' ');
const el=(t,o={},children='')=>`<${t} ${attr(o)}>${children}</${t}>`;
const color=v=>'FF'+(v.length===4 ? v.slice(1).split('').map(x=>x+x).join('') : v.slice(1));
function paths(d) {
  const result=[]; let points=[],closed=false;
  const finish=()=>{if(points.length)result.push({points,closed});points=[];closed=false;};
  svgpath(d).abs().unshort().unarc().iterate(s=> {
    const previous=points.at(-1);
    if(s[0]==='M'){finish();points.push({x:s[1],y:s[2]});}
    else if(s[0]==='Z'){closed=true;finish();}
    else if(['L','H','V'].includes(s[0]))points.push({x:s[0]==='V'?previous.x:s[1],y:s[0]==='H'?previous.y:s.at(-1)});
    else if(s[0]==='C'||s[0]==='Q') {
      const end={x:s.at(-2),y:s.at(-1)};
      const c1=s[0]==='C'?{x:s[1],y:s[2]}:{x:previous.x+2/3*(s[1]-previous.x),y:previous.y+2/3*(s[2]-previous.y)};
      const c2=s[0]==='C'?{x:s[3],y:s[4]}:{x:end.x+2/3*(s[1]-end.x),y:end.y+2/3*(s[2]-end.y)};
      previous.outRotation=n(Math.atan2(c1.y-previous.y,c1.x-previous.x));previous.outDistance=n(Math.hypot(c1.x-previous.x,c1.y-previous.y));
      end.inRotation=n(Math.atan2(c2.y-end.y,c2.x-end.x));end.inDistance=n(Math.hypot(c2.x-end.x,c2.y-end.y));points.push(end);
    } else throw Error(`Unsupported SVG command ${s[0]}`);
  });finish();return result;
}
const channels=['anger','sadness','happiness','frustration','confusion','concern','surprise','valence','arousal','sarcasm'];
const expressions=['neutral','happy','concerned','confused','sad','frustrated','angry','surprised'];
const channel={happy:'happiness',concerned:'concern',confused:'confusion',sad:'sadness',frustrated:'frustration',angry:'anger',surprised:'surprise'};
function character(variant,index) {
  const source=fs.readFileSync(path.join(root,`frontend/public/avatars/${variant}.svg`),'utf8');
  const gradients=Object.fromEntries([...source.matchAll(/<(?:linear|radial)Gradient id="(.*?)"[^>]*>(.*?)<\/(?:linear|radial)Gradient>/gs)].map(m=>[m[1],[...m[2].matchAll(/<stop ([^>]*)\/>/g)].map(s=>Object.fromEntries([...s[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(a=>[a[1],a[2]])))]));
  const tags=[...source.replace(/<defs>.*?<\/defs>/s,'').matchAll(/<(path|ellipse|circle) ([^>]*)\/>/g)].map(m=>({tag:m[1],a:Object.fromEntries([...m[2].matchAll(/([\w-]+)="([^"]*)"/g)].map(a=>[a[1],a[2]]))}));
  const female=variant==='female';
  const nodes={body:id(),head:id(),eyes:id(),browL:id(),browR:id(),mouth:id(),arms:id(),open:id(),teeth:id(),presence:id(),breath:id(),headDrift:id(),blink:id(),gaze:id(),browLift:id(),mouthTone:id()};
  const components={body:[],head:[],eyes:[],mouth:[],arms:[]};
  const draw=(tag,a,pivot=[0,0],extra={})=> {
    const shapeId=extra.id??id();let geometry;
    if(tag==='path')geometry=paths(a.d).map(p=>el('PointsPath',{isClosed:p.closed},p.points.map(p=>el('CubicDetachedVertex',p)).join(''))).join('');
    else geometry=el('Ellipse',{x:a.cx,y:a.cy,width:2*Number(a.rx??a.r),height:2*Number(a.ry??a.r)});
    const paint=(kind,value)=> {
      if(!value||value==='none')return '';
      const stops=gradients[value.match(/url\(#(.*?)\)/)?.[1]];
      const pigment=stops?el('LinearGradient',{startX:75,startY:40,endX:170,endY:240},stops.map((s,i)=>el('GradientStop',{colorValue:color(s['stop-color']),position:s.offset??i/(stops.length-1)})).join('')):el('SolidColor',{colorValue:color(value)});
      return el(kind,kind==='Stroke'?{thickness:a['stroke-width']??1,cap:'round',join:'round'}:{},pigment);
    };
    return el('Shape',{id:shapeId,x:-pivot[0],y:-pivot[1],opacity:a.opacity??1,...extra},geometry+paint('Fill',a.fill??(a.stroke?'none':'#000'))+paint('Stroke',a.stroke));
  };
  const browIndex=female?7:10, eyeStart=female?8:11,eyeEnd=female?15:17,mouthStart=female?17:19,mouthEnd=female?18:19;
  tags.forEach(({tag,a},i)=> {
    if(i===browIndex){
      const p=paths(a.d);
      const brows=p.map((part,j)=>el('Node',{id:nodes[j?'browR':'browL'],name:j?'Right brow':'Left brow',x:j?23:-23,y:-6},el('Shape',{x:j?-153:-107,y:-94},el('PointsPath',{},part.points.map(v=>el('CubicDetachedVertex',v)).join(''))+el('Stroke',{thickness:a['stroke-width'],cap:'round'},el('SolidColor',{colorValue:color(a.stroke)})))));
      components.head.push(el('Node',{id:nodes.browLift,name:'Sarcasm brow offset'},brows.join('')));return;
    }
    let group=(i>=eyeStart&&i<=eyeEnd)?'eyes':(i>=mouthStart&&i<=mouthEnd)?'mouth':(!female&&i>=22&&i<=24||female&&i>=21&&i<=23)?'arms':(!female&&i>=5&&i<=21||female&&i>=4&&i<=20||female&&i===24)?'head':'body';
    const pivot=group==='eyes'?[130,109]:group==='mouth'?[130,140]:group==='arms'?[130,230]:group==='head'?[130,100]:[130,180];
    components[group].push(draw(tag,a,pivot));
    if(i===eyeStart)components.head.push('EYES');
    if(i===mouthStart)components.head.push('MOUTH');
  });
  const teeth=draw('ellipse',{cx:130,cy:138,rx:9,ry:3,fill:'#fff0e6'},[130,140],{id:nodes.teeth,opacity:0});
  const opening=draw('ellipse',{cx:130,cy:142,rx:10,ry:8,fill:'#532e36'},[130,140],{id:nodes.open,opacity:0});
  const mouth=el('Node',{id:nodes.mouthTone,name:'Valence mouth offset'},el('Node',{id:nodes.mouth,name:'Mouth',x:0,y:40},teeth+opening+components.mouth.reverse().join('')));
  const eyes=el('Node',{id:nodes.gaze,name:'Idle eye drift'},el('Node',{id:nodes.blink,name:'Blink',y:9},el('Node',{id:nodes.eyes,name:'Eyes'},components.eyes.reverse().join(''))));
  const head=el('Node',{id:nodes.headDrift,name:'Idle head drift'},el('Node',{id:nodes.head,name:'Head',x:0,y:-80},components.head.reverse().join('').replace('EYES',eyes).replace('MOUTH',mouth)));
  const arms=el('Node',{id:nodes.arms,name:'Resting arms and hands',x:0,y:50},components.arms.reverse().join(''));
  const body=el('Node',{id:nodes.presence,name:'Typing attention'},el('Node',{id:nodes.breath,name:'Breathing',x:130,y:180},el('Node',{id:nodes.body,name:'Upper body'},arms+head+components.body.reverse().join(''))));
  const allInputs=[...channels,'attentive','motion'];
  const inputs=Object.fromEntries(allInputs.map(c=>[c,id()]));const machineId=id(),artId=id(),styleId=id();
  // Three independent layers stagger facial, head and posture transitions.
  const faceKeys=[[nodes.browL,15],[nodes.browR,15],[nodes.browL,14],[nodes.browR,14],[nodes.eyes,17],[nodes.mouth,17],[nodes.mouth,15],[nodes.mouth,14],[nodes.open,18],[nodes.open,17],[nodes.teeth,18]];
  const face={neutral:[0,0,94,94,1,1,0,140,0,1,0],happy:[-.12,.12,90,90,.45,1.4,0,138,.8,.7,.8],concerned:[-.22,.22,92,92,.9,-.8,0,143,0,1,0],confused:[-.25,-.12,89,96,.85,.8,-.12,141,0,1,0],sad:[-.28,.28,96,96,.65,-1,0,144,0,1,0],frustrated:[.2,-.2,96,96,.5,-.7,0,141,.65,.35,.5],angry:[.32,-.32,97,97,.65,-1,0,140,1,.55,1],surprised:[-.1,.1,84,84,1.65,.5,0,142,1,1.45,0]};
  const headKeys=[[nodes.head,15],[nodes.head,14]];
  const heads={neutral:[0,100],happy:[-.06,96],concerned:[.06,102],confused:[.15,100],sad:[-.07,108],frustrated:[-.05,99],angry:[0,96],surprised:[-.05,95]};
  const bodyKeys=[[nodes.body,15],[nodes.body,17],[nodes.arms,14],[nodes.arms,15]];
  const bodies={neutral:[0,1,230,0],happy:[-.025,1.025,225,-.025],concerned:[.02,.99,228,.025],confused:[.025,1,224,.065],sad:[-.02,.96,234,.02],frustrated:[-.02,1.02,222,-.035],angry:[0,1.045,218,-.05],surprised:[.02,1.025,217,.03]};
  const timelines=[];
  let layers=[['Face',faceKeys,face,300],['Head',headKeys,heads,400],['Posture',bodyKeys,bodies,700]].map(([name,keys,values,duration])=> {
    const animationIds=Object.fromEntries(expressions.map(e=>[e,id()]));const states=Object.fromEntries(expressions.map(e=>[e,id()]));
    expressions.forEach(e=>timelines.push(el('LinearAnimation',{id:animationIds[e],name:`${name} ${e}`,duration:60,fps:60},keys.map(([objectId,propertyKey],i)=>el('KeyedObject',{objectId},el('KeyedProperty',{propertyKey},el('KeyFrameDouble',{frame:0,value:values[e][i]-(name==='Face'&&[2,3,7].includes(i)?100:name==='Head'&&i===1?180:name==='Posture'&&i===2?180:0)})))).join(''))));
    const transitions=(from)=>expressions.filter(e=>e!==from).map(e=> {
      // Priority prevents competing positive inputs from making the graph oscillate.
      const ordered=['anger','frustration','surprise','sadness','concern','confusion','happiness'];
      const cond=e==='neutral'?ordered.map(c=>[c,'lessThanOrEqual',.001]):[[channel[e],'greaterThan',.001],...ordered.slice(0,ordered.indexOf(channel[e])).map(c=>[c,'lessThanOrEqual',.001])];
      return el(from==='neutral'?'StateTransition':'BlendStateTransition',{stateToId:states[e],duration},cond.map(([c,opValue,value])=>el('TransitionNumberCondition',{inputId:inputs[c],opValue,value})).join(''));
    }).join('');
    return el('StateMachineLayer',{name,id:id()},el('AnyState',{x:0,y:-120})+el('ExitState',{x:400,y:-120})+el('EntryState',{x:0,y:0},el('StateTransition',{stateToId:states.neutral}))+expressions.map((e,i)=>e==='neutral'?el('AnimationState',{id:states[e],animationId:animationIds[e],x:200,y:i*120},transitions(e)):el('BlendState1DInput',{id:states[e],inputId:inputs[channel[e]],x:200,y:i*120},el('BlendAnimation1D',{animationId:animationIds.neutral,value:0})+el('BlendAnimation1D',{animationId:animationIds[e],value:100})+transitions(e))).join(''));
  }).join('');
  const timeline=(name,objectId,key,frames,duration=300,extra=[])=> {
    const animationId=id();timelines.push(el('LinearAnimation',{id:animationId,name,fps:60,duration,loopValue:'loop'},[[objectId,key,frames],...extra].map(([objectId,propertyKey,frames])=>el('KeyedObject',{objectId},el('KeyedProperty',{propertyKey},frames.map(([frame,value])=>el('KeyFrameDouble',{frame,value,interpolationType:'linear'})).join('')))).join('')));return animationId;
  };
  const blendLayer=(name,input,points)=> {
    const stateId=id();layers+=el('StateMachineLayer',{name,id:id()},el('AnyState',{x:0,y:-120})+el('ExitState',{x:400,y:-120})+el('EntryState',{x:0},el('StateTransition',{stateToId:stateId}))+el('BlendState1DInput',{id:stateId,inputId:inputs[input],x:200},points.map(([animationId,value])=>el('BlendAnimation1D',{animationId,value})).join('')));
  };
  blendLayer('Blink presence','motion',[[timeline('No blink',nodes.blink,17,[[0,1]]),0],[timeline('Natural blink',nodes.blink,17,[[0,1],[162,1],[168,.035],[174,1],[300,1]]),100]]);
  blendLayer('Eye movement','motion',[[timeline('Still gaze',nodes.gaze,13,[[0,0]],360),0],[timeline('Soft gaze',nodes.gaze,13,[[0,0],[90,.65],[240,-.55],[360,0]],360),100]]);
  blendLayer('Head movement','motion',[[timeline('Still head',nodes.headDrift,15,[[0,0]],360),0],[timeline('Soft head',nodes.headDrift,15,[[0,0],[90,.009],[240,-.008],[360,0]],360),100]]);
  blendLayer('Valence tone','valence',[[timeline('Negative mouth tone',nodes.mouthTone,14,[[0,1.2]]),-100],[timeline('Neutral mouth tone',nodes.mouthTone,14,[[0,0]]),0],[timeline('Positive mouth tone',nodes.mouthTone,14,[[0,-1.2]]),100]]);
  blendLayer('Sarcasm hint','sarcasm',[[timeline('Relaxed brow offset',nodes.browLift,15,[[0,0]]),0],[timeline('Asymmetric brow offset',nodes.browLift,15,[[0,.045]]),100]]);
  const restingBreath=timeline('Still breathing',nodes.breath,17,[[0,1]],240);
  const calmBreath=timeline('Calm breathing',nodes.breath,17,[[0,1],[120,1.008],[240,1]],240);
  const activeBreath=timeline('Alert breathing',nodes.breath,17,[[0,1],[120,1.015],[240,1]],240);
  const breathRest=id(),breathActive=id();
  layers+=el('StateMachineLayer',{name:'Breathing presence',id:id()},el('AnyState',{x:0,y:-120})+el('ExitState',{x:400,y:-120})+el('EntryState',{x:0},el('StateTransition',{stateToId:breathActive}))+el('AnimationState',{id:breathRest,animationId:restingBreath,x:200},el('StateTransition',{stateToId:breathActive,duration:250},el('TransitionNumberCondition',{inputId:inputs.motion,opValue:'greaterThan',value:0})))+el('BlendState1DInput',{id:breathActive,inputId:inputs.arousal,x:200,y:150},el('BlendAnimation1D',{animationId:calmBreath,value:0})+el('BlendAnimation1D',{animationId:activeBreath,value:100})+el('BlendStateTransition',{stateToId:breathRest,duration:250},el('TransitionNumberCondition',{inputId:inputs.motion,opValue:'lessThanOrEqual',value:0}))));
  const attentionRest=timeline('Resting height',nodes.presence,14,[[0,0]],300,[[nodes.gaze,14,[[0,0]]]]),attentionUp=timeline('Attentive height',nodes.presence,14,[[0,-7]],300,[[nodes.gaze,14,[[0,-1.5]]]]);
  const attentionIdle=id(),attentionTyping=id();
  layers+=el('StateMachineLayer',{name:'Typing attention',id:id()},el('AnyState',{x:0,y:-120})+el('ExitState',{x:400,y:-120})+el('EntryState',{x:0},el('StateTransition',{stateToId:attentionIdle}))+el('AnimationState',{id:attentionIdle,animationId:attentionRest,x:200},el('StateTransition',{stateToId:attentionTyping,duration:400},el('TransitionNumberCondition',{inputId:inputs.attentive,opValue:'greaterThan',value:0})))+el('BlendState1DInput',{id:attentionTyping,inputId:inputs.attentive,x:200,y:150},el('BlendAnimation1D',{animationId:attentionRest,value:0})+el('BlendAnimation1D',{animationId:attentionUp,value:100})+el('BlendStateTransition',{stateToId:attentionIdle,duration:400},el('TransitionNumberCondition',{inputId:inputs.attentive,opValue:'lessThanOrEqual',value:0}))));
  const machine=el('StateMachine',{id:machineId,name:'TemperEmotion'},allInputs.map(c=>el('StateMachineNumber',{id:inputs[c],name:c,value:c==='arousal'?15:c==='motion'?100:0})).join('')+layers);
  return el('Artboard',{id:artId,name:female?'TemperFemale':'TemperMale',width:260,height:260,x:index*300,styleId,defaultStateMachineId:machineId},el('LayoutComponentStyle',{id:styleId})+body+machine+timelines.join(''));
}
const xml=el('Rive',{version:1,kind:'fragment'},character('male',0)+character('female',1));
fs.writeFileSync(path.join(root,'assets/avatars/temper/scene.rml'),xml.replace(/></g,'>\n<')+'\n');
console.log('Generated two original artboards, semantic expressions, layered idle motion and typing attention.');


