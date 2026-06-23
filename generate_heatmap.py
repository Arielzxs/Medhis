
import subprocess
import datetime
import os
import re
from collections import defaultdict
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from matplotlib.colors import ListedColormap, BoundaryNorm
import numpy as np

def normalize_name(name):
    """标准化名字:小写+移除非字母数字字符"""
    return re.sub(r'[^a-z0-9]', '', name.lower())

def get_all_contributors(git_dir):
    """获取所有贡献者列表并合并相似名字"""
    cmd = ['git', '--git-dir', git_dir, 'log', '--all', '--pretty=format:%an', '--date=short']
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', check=True)
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"Git命令执行失败: {e.stderr}")
    
    raw_contributors = result.stdout.splitlines()
    if not raw_contributors:
        return [], {}

    # 创建名字分组字典
    name_groups = defaultdict(list)
    for name in raw_contributors:
        if name.strip():
            normalized = normalize_name(name)
            name_groups[normalized].append(name)
            
    # 选择每个组中最长的名字作为代表（通常更完整）
    merged_contributors = []
    canonical_names = {}
    for group in name_groups.values():
        # 选择组内最长的名字作为标准名称，如果长度相同取第一个
        canonical_name = max(group, key=len)
        merged_contributors.append(canonical_name)
        for name in group:
            canonical_names[name] = canonical_name
            
    return sorted(set(merged_contributors)), canonical_names

def get_commit_data(git_dir, start_date, end_date):
    """
    获取指定日期范围内的提交数据
    返回格式: { 'author': { 'date_str': count } }
    """
    # 使用 iso 格式获取日期和作者
    cmd = [
        'git', '--git-dir', git_dir, 'log', '--all',
        '--pretty=format:%ad|||%an',
        '--date=short',
        f'--since={start_date}',
        f'--until={end_date}'
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', check=True)
    except subprocess.CalledProcessError as e:
        print(f"Warning: Git log failed: {e.stderr}")
        return {}

    lines = result.stdout.strip().split('\n')
    commit_data = defaultdict(lambda: defaultdict(int))
    
    for line in lines:
        if '|||' not in line:
            continue
        date_str, author = line.split('|||', 1)
        # 标准化作者名以便后续映射，这里先存原始名，后面再处理
        commit_data[author][date_str] += 1
        
    return commit_data

def generate_heatmap(git_dir='.git', output_file='commit_heatmap.png', days=365):
    """
    生成代码提交热力图
    :param git_dir: .git 目录路径
    :param output_file: 输出图片文件名
    :param days: 回溯天数
    """
    if not os.path.exists(git_dir):
        raise FileNotFoundError(f"未找到 git 目录: {git_dir}")

    end_date = datetime.datetime.now()
    start_date = end_date - datetime.timedelta(days=days)
    
    start_date_str = start_date.strftime('%Y-%m-%d')
    end_date_str = end_date.strftime('%Y-%m-%d')
    
    print(f"正在分析从 {start_date_str} 到 {end_date_str} 的提交记录...")
    
    # 1. 获取原始提交数据
    raw_data = get_commit_data(git_dir, start_date_str, end_date_str)
    
    if not raw_data:
        print("在此期间未找到任何提交记录。")
        return

    # 2. 获取所有贡献者并建立映射
    all_contributors, name_map = get_all_contributors(git_dir)
    
    if not all_contributors:
        print("未找到贡献者。")
        return

    # 3. 整理数据矩阵
    # 行: 贡献者, 列: 日期
    contributors = sorted(all_contributors)
    
    # 生成连续日期列表
    date_list = []
    curr_date = start_date
    while curr_date <= end_date:
        date_list.append(curr_date.strftime('%Y-%m-%d'))
        curr_date += datetime.timedelta(days=1)
        
    date_to_index = {d: i for i, d in enumerate(date_list)}
    contributor_to_index = {c: i for i, c in enumerate(contributors)}
    
    # 初始化矩阵
    matrix = np.zeros((len(contributors), len(date_list)))
    
    for author, dates in raw_data.items():
        # 映射到标准名称
        std_name = name_map.get(author, author)
        if std_name in contributor_to_index:
            row_idx = contributor_to_index[std_name]
            for date_str, count in dates.items():
                if date_str in date_to_index:
                    col_idx = date_to_index[date_str]
                    matrix[row_idx][col_idx] += count

    # 4. 绘制热力图
    plt.figure(figsize=(20, 8))
    
    # 使用 viridis 或 YlOrRd 等适合热力图的 colormap
    cmap = plt.cm.YlOrRd
    
    # 处理稀疏数据，让0值显示为白色或浅色
    norm = BoundaryNorm(np.arange(-0.5, np.max(matrix) + 1.5, 1), cmap.N)
    
    plt.imshow(matrix, aspect='auto', cmap=cmap, interpolation='nearest')
    
    # 设置轴标签
    plt.yticks(ticks=range(len(contributors)), labels=contributors, fontsize=8)
    
    # 设置X轴日期标签，每隔一定天数显示一个，避免拥挤
    step = max(1, len(date_list) // 10)
    tick_indices = range(0, len(date_list), step)
    tick_labels = [date_list[i] for i in tick_indices]
    plt.xticks(ticks=tick_indices, labels=tick_labels, rotation=45, ha='right', fontsize=8)
    
    plt.title(f'Code Commit Heatmap ({start_date_str} to {end_date_str})', fontsize=14)
    plt.xlabel('Date')
    plt.ylabel('Contributor')
    
    # 添加颜色条
    cbar = plt.colorbar()
    cbar.set_label('Number of Commits')
    
    plt.tight_layout()
    plt.savefig(output_file, dpi=150, bbox_inches='tight')
    print(f"热力图已保存至: {output_file}")

if __name__ == '__main__':

    import datetime
    today = datetime.datetime.now().strftime('%Y%m%d')
    output_filename = f'heatmap_{today}.png'

    # 默认在当前目录下的 .git 文件夹查找
    # 如果需要指定其他路径，修改 git_dir 参数
    try:
        generate_heatmap(git_dir='.git', output_file=output_filename,days=7)
    except Exception as e:
        print(f"错误: {e}")
