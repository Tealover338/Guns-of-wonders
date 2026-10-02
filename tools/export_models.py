import json, pathlib, uuid, copy, base64, math
def save(p,d):
    p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
def uid(): return str(uuid.uuid4())
def rotate(p,origin,angles):
    x,y,z=[p[i]-origin[i] for i in range(3)]
    for axis,a in enumerate(angles):
        c,s=math.cos(math.radians(a)),math.sin(math.radians(a))
        if axis==0:y,z=y*c-z*s,y*s+z*c
        elif axis==1:x,z=x*c+z*s,-x*s+z*c
        else:x,y=x*c-y*s,x*s+y*c
    return [x+origin[0],y+origin[1],z+origin[2]]
def group(name,origin=[0,0,0],rotation=[0,0,0]):
    return dict(name=name,uuid=uid(),origin=origin,rotation=rotation,export=True,visibility=True,isOpen=True)
def node(g,children):return dict(uuid=g['uuid'],isOpen=True,children=children)
def arm(name,wrist,rot):
    x,y,z=wrist
    return dict(name=name,type='cube',uuid=uid(),from_=[x-1.5,y-12,z-2],to=[x+1.5,y,z+2],origin=wrist,rotation=rot,box_uv=False,faces={})
def arm_faces(left):
    # Standard 64x64 Minecraft skin arm regions, with a 3-pixel arm width.
    u,v=(32,48) if left else (40,16)
    uv={'west':[u,v+4,u+4,v+16],'north':[u+4,v+4,u+7,v+16],'east':[u+7,v+4,u+11,v+16],'south':[u+11,v+4,u+14,v+16],'up':[u+4,v,u+7,v+4],'down':[u+7,v,u+10,v+4]}
    return {k:dict(uv=q,texture=False) for k,q in uv.items()}
def quads(e,ancestors,textures,res,skin=False):
    a,b=e['from'],e['to']; x,y,z=a; X,Y,Z=b
    vertices={'north':[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],'south':[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],'west':[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],'east':[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],'up':[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],'down':[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]}
    result=[]
    for face,f in e.get('faces',{}).items():
        ti=f.get('texture');
        if ti is None or (ti is False and not skin):continue
        tex=textures[int(ti)] if not skin else {}
        w,h=(64,64) if skin else (tex.get('uv_width',res['width']),tex.get('uv_height',res['height']))
        u,v,U,V=f['uv']; uvs=[[U/w,V/h],[u/w,V/h],[u/w,v/h],[U/w,v/h]]
        turns=(f.get('rotation',0)//90)%4; uvs=uvs[-turns:]+uvs[:-turns] if turns else uvs
        vv=[]
        for p in vertices[face]:
            p=rotate(p,e.get('origin',[0,0,0]),e.get('rotation',[0,0,0]))
            for g in reversed(ancestors):p=rotate(p,g.get('origin',[0,0,0]),g.get('rotation',[0,0,0]))
            vv.append(p)
        d=[vv[1][i]-vv[0][i] for i in range(3)]; t=[vv[2][i]-vv[0][i] for i in range(3)]
        n=[d[1]*t[2]-d[2]*t[1],d[2]*t[0]-d[0]*t[2],d[0]*t[1]-d[1]*t[0]]; l=math.sqrt(sum(a*a for a in n))
        if l<1e-8:continue
        result.append(dict(texture='skin' if skin else str(ti),vertices=vv,uv=uvs,normal=[a/l for a in n]))
    return result

import argparse
parser=argparse.ArgumentParser(description='Export posed Guns of Wonders Blockbench projects without changing them.')
parser.add_argument('models',type=pathlib.Path)
args=parser.parse_args()
PROJECT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=PROJECT/'src/main/resources/assets/gunsofwonders'
for path in sorted(args.models.glob('*.bbmodel')):
    if path.stem in {'hands','glocki'}:continue
    m=json.loads(path.read_text(encoding='utf-8'));ident=path.stem.replace("'",'').replace('-','_')
    elements={e['uuid']:e for e in m['elements']};groups={g['uuid']:g for g in m.get('groups',[])}
    mesh=[];arms=[];wrist_points={};arm_poses={}
    def walk(n,parents):
        if isinstance(n,str):
            e=elements[n]
            if e.get('type')!='cube' or e.get('export') is False or e.get('visibility') is False:return
            skin=e.get('name') in ['right_arm','left_arm']
            if skin:
                point=e.get('origin',[0,0,0])
                for g in reversed(parents):point=rotate(point,g.get('origin',[0,0,0]),g.get('rotation',[0,0,0]))
                def transform(point):
                    point=rotate(point,e.get('origin',[0,0,0]),e.get('rotation',[0,0,0]))
                    for parent in reversed(parents):point=rotate(point,parent.get('origin',[0,0,0]),parent.get('rotation',[0,0,0]))
                    return point
                wrist=[(e['from'][0]+e['to'][0])/2,e['to'][1],(e['from'][2]+e['to'][2])/2]
                point=transform(wrist)
                axes=[]
                for axis in range(3):
                    endpoint=wrist.copy();endpoint[axis]+=1
                    axes.append([a-b for a,b in zip(transform(endpoint),point)])
                wrist_points[e['name']]=point
                arm_poses[e['name']]=dict(wrist=point,axes=axes,width=e['to'][0]-e['from'][0])
            (arms if skin else mesh).extend(quads(e,parents,m['textures'],m['resolution'],skin));return
        g=groups.get(n['uuid'],n)
        if g.get('name') in {'camera','idle_view','iron_view'}:return
        if g.get('export') is False or g.get('visibility') is False:return
        for c in n.get('children',[]):walk(c,parents+[g])
    for n in m['outliner']:walk(n,[])
    byname={g['name']:g for g in groups.values()}
    camera=byname['camera']['origin'];grip=wrist_points.get('right_arm',byname['righthand']['origin'])
    support=wrist_points.get('left_arm',byname['lefthand']['origin'])
    animations=[a for a in m.get('animations',[]) if a['name']=='idle']
    if not animations:raise ValueError(f'{path}: missing idle animation')
    anim=animations[0];root_id=byname['root']['uuid']
    keys=anim['animators'][root_id]['keyframes']
    frames=[dict(time=k['time'],position=[float(k['data_points'][0][axis]) for axis in ['x','y','z']]) for k in keys if k['channel']=='position']
    if len(frames)<2 or anim['length']<=0:raise ValueError(f'{path}: idle needs at least two root position keys')
    frames.sort(key=lambda k:k['time'])
    points=[v for q in mesh for v in q['vertices']]
    bounds=[[min(v[i] for v in points) for i in range(3)],[max(v[i] for v in points) for i in range(3)]]
    save(ASSETS/'guns'/f'{ident}.json',dict(camera=camera,grip=grip,supportGrip=support,rightArm=arm_poses['right_arm'],leftArm=arm_poses['left_arm'],bounds=bounds,quads=mesh,arms=arms,idle=frames,idleLength=anim['length']))
    texdir=ASSETS/'textures/guns'/ident;texdir.mkdir(parents=True,exist_ok=True)
    for i,t in enumerate(m['textures']):
        (texdir/f'{i}.png').write_bytes(base64.b64decode(t['source'].split(',',1)[1]))
    print(ident)
