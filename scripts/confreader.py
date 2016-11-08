import re
import sys

class NoMoreLinesException(Exception):
    pass

class NoMoreCharsException(Exception):
    pass

def read_lines(fobj, nlines):

    try:
        return ''.join([next(fobj) for i in range(0, nlines)])
    except StopIteration:
        raise NoMoreLinesException()

def read_chars(fobj, nchars):
    try:
        return ''.join([fobj.read(1) for i in range(0, nchars)])
    except StopIteration:
        raise NoMoreCharsException()

def read_conf_into_dict(filename):

    re0 = re.compile(r'((?P<comment>^\s*#.*)|(?P<keyval4>^\s*[^\s]+\s*:[1-9][0-9]*[lL]=\s*.+)|(?P<keyval3>^\s*[^\s]+\s*:[1-9][0-9]*[Cc]=\s*.+)|(?P<keyval2>^\s*[^\s]+\s*:=\s*.+)|(?P<keyval>^\s*[^\s]+\s*=\s*.+)|(?P<blankline>^\s*))')
    re4 = re.compile(r'^\s*(?P<key>[^\s]+?)\s*:(?P<numlines>[1-9][0-9]*)[lL]=(?P<value>\s*.+)')
    re3 = re.compile(r'^\s*(?P<key>[^\s]+?)\s*:(?P<numchars>[1-9][0-9]*)[cC]=(?P<value>\s*.+)')
    re2 = re.compile(r'^\s*(?P<key>[^\s]+?)\s*:=(?P<value>\s*.+)')
    re1 = re.compile(r'^\s*(?P<key>[^\s]+?)\s*=(?P<value>\s*.+)')

    conf_dict = {}

    with open(filename) as fobj:
        try:
            line = next(fobj)
            while True:
                match = re0.match(line)

                if match:

                    # keyval4 can span multiple lines
                    if match.groupdict()['keyval4']:
                        m4 = re4.match(line)
                        key = m4.groupdict()['key']
                        nlines = int(m4.groupdict()['numlines'])
                        value = m4.groupdict()['value'] + '\n' + read_lines(fobj, nlines-1)
                        conf_dict[key] = value.strip('\n')

                    # multiple characters
                    elif match.groupdict()['keyval3']:
                        m3 = re3.match(line)
                        key = m3.groupdict()['key']
                        nchars = int(m3.groupdict()['numchars'])
                        value = m3.groupdict()['value']
                        if nchars > len(value):
                            value = value + '\n' + read_chars(fobj, nchars-len(value)-1)
                            conf_dict[key] = value.strip('\n')
                        elif nchars == len(value):
                            conf_dict[key] = value.strip('\n')
                        elif nchars < len(value):
                            conf_dict[key] = value[0:nchars]
                            line = value[nchars:]
                            continue
                    # include spaces
                    elif match.groupdict()['keyval2']:
                        m2 = re2.match(line)
                        key = m2.groupdict()['key']
                        value = m2.groupdict()['value']
                        conf_dict[key] = value.strip('\n')

                    elif match.groupdict()['keyval']:
                        #print('keyval', match.groupdict()['keyval'])
                        m1 = re1.match(line)
                        key = m1.groupdict()['key']
                        value = m1.groupdict()['value']
                        value = value.strip('\n').strip()
                        #for quoted string
                        if value.startswith("'") and value.endswith("'"):
                            #value = quote_str(value[1:-1])
                            value = value[1:-1]

                        conf_dict[key] = value

                line = next(fobj)

        except StopIteration:
            pass
    return conf_dict

def main(filepath):
    conf_dict = read_conf_into_dict(filepath)
    for key in sorted(conf_dict.keys()):
        print('|{0}|\t:\t|{1}|'.format(key, conf_dict[key].replace(' ', '.')))

if __name__ == '__main__':
    main(sys.argv[1])

